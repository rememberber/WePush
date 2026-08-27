package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.service.domain.AccountDefinition;
import com.fangxuele.wepush.next.service.domain.AccountRepository;
import com.fangxuele.wepush.next.service.domain.AudienceDefinition;
import com.fangxuele.wepush.next.service.domain.AudienceRepository;
import com.fangxuele.wepush.next.service.domain.IdempotencyRecord;
import com.fangxuele.wepush.next.service.domain.JobDefinition;
import com.fangxuele.wepush.next.service.domain.JobRepository;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.MessageDefinition;
import com.fangxuele.wepush.next.service.domain.MessageRepository;
import com.fangxuele.wepush.next.service.domain.RunDefinition;
import com.fangxuele.wepush.next.service.domain.RunEventRecord;
import com.fangxuele.wepush.next.service.domain.RunRepository;
import com.fangxuele.wepush.next.service.domain.RunResultRepository;
import com.fangxuele.wepush.next.service.domain.RunSnapshot;
import com.fangxuele.wepush.next.service.domain.RunStatus;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.time.DateTimeException;

public final class RunApplicationService {
    private static final String IDEMPOTENCY_SCOPE = "CREATE_RUN";

    private final WorkspaceRepository workspaces;
    private final AccountRepository accounts;
    private final MessageRepository messages;
    private final AudienceRepository audiences;
    private final JobRepository jobs;
    private final RunRepository runs;
    private final RunResultRepository results;
    private final ProviderRegistry providers;
    private final JsonCodec json;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final RunEventPublisher events;
    private final RunDispatcher dispatcher;
    private final CursorCodec cursors;
    private final Clock clock;

    public RunApplicationService(WorkspaceRepository workspaces, AccountRepository accounts,
                                 MessageRepository messages, AudienceRepository audiences,
                                 JobRepository jobs, RunRepository runs, RunResultRepository results,
                                 ProviderRegistry providers,
                                 JsonCodec json, ResourceIdGenerator ids, TransactionRunner transactions,
                                 RunEventPublisher events, RunDispatcher dispatcher,
                                 CursorCodec cursors, Clock clock) {
        this.workspaces = workspaces;
        this.accounts = accounts;
        this.messages = messages;
        this.audiences = audiences;
        this.jobs = jobs;
        this.runs = runs;
        this.results = results;
        this.providers = providers;
        this.json = json;
        this.ids = ids;
        this.transactions = transactions;
        this.events = events;
        this.dispatcher = dispatcher;
        this.cursors = cursors;
        this.clock = clock;
    }

    public CreationResult create(WorkspaceId workspaceId, String jobId, String idempotencyKey,
                                 CreateRun command) {
        return create(workspaceId, jobId, idempotencyKey, command, false);
    }

    public CreationResult createScheduled(WorkspaceId workspaceId, String jobId, String idempotencyKey,
                                          CreateRun command) {
        return create(workspaceId, jobId, idempotencyKey, command, true);
    }

    private CreationResult create(WorkspaceId workspaceId, String jobId, String idempotencyKey,
                                  CreateRun command, boolean trustedSchedule) {
        if (!command.dryRun() && !trustedSchedule) requireLiveConfirmation(workspaceId, jobId,
                command.confirmationToken());
        String key = ApplicationSupport.text(idempotencyKey, "Idempotency-Key");
        JsonDocument request = json.canonicalize(Map.of(
                "jobId", ApplicationSupport.text(jobId, "jobId"),
                "dryRun", command.dryRun(),
                "policyOverrides", command.policyOverrides() == null ? Map.of() : command.policyOverrides(),
                "reason", command.reason() == null ? "manual" : command.reason(),
                "confirmation", command.confirmationToken() == null ? "" :
                        ApplicationSupport.sha256(command.confirmationToken())));
        String keyHash = ApplicationSupport.sha256(key);
        String requestHash = ApplicationSupport.sha256(request.value());

        Created created = transactions.required(() -> createInTransaction(
                workspaceId, jobId, keyHash, requestHash, command));
        if (!created.replayed()) {
            events.publish(created.event());
            dispatcher.dispatch(workspaceId, created.run().id());
        }
        return new CreationResult(created.run(), created.replayed());
    }

    public LiveConfirmation confirm(WorkspaceId workspaceId, String jobId) {
        ConfirmationContext context = confirmationContext(workspaceId, jobId);
        Instant expiresAt = clock.instant().plus(Duration.ofMinutes(5));
        String token = cursors.encode("live-run-confirm-v1", context.fingerprint() + "\0" + expiresAt);
        Map<?, ?> policies = json.read(context.job().policies(), Map.class);
        Map<?, ?> concurrency = section(policies, "concurrency");
        Map<?, ?> rateLimit = section(policies, "rateLimit");
        int target = integer(concurrency.get("target"), 1);
        long permits = longValue(rateLimit.get("permits"), Long.MAX_VALUE);
        String period = String.valueOf(rateLimit.containsKey("period") ? rateLimit.get("period") : "PT1S");
        return new LiveConfirmation(context.job().id(), context.job().name(),
                context.account().provider().providerId(), context.account().provider().implementationVersion(),
                context.account().id(), context.account().name(), context.audience().id(),
                context.audience().name(), context.audience().recordCount(),
                json.read(context.job().policies(), Object.class), target, permits, period,
                context.audience().recordCount(), expiresAt, token);
    }

    public RetryConfirmation confirmRetry(WorkspaceId workspaceId, String sourceRunId, Set<String> states) {
        Set<String> normalized = retryStates(states);
        RunDefinition source = get(workspaceId, sourceRunId);
        if (!source.status().terminal()) {
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "RUN_NOT_TERMINAL",
                    "Only a terminal Run can be retried");
        }
        long count = results.countByStates(workspaceId, sourceRunId, normalized);
        if (count == 0) throw new ApplicationProblem(ApplicationProblem.Kind.UNPROCESSABLE,
                "NO_RETRYABLE_ITEMS", "No Run items match the requested retry states");
        Instant expiresAt = clock.instant().plus(Duration.ofMinutes(5));
        String fingerprint = retryFingerprint(workspaceId, source, normalized, count);
        String token = cursors.encode("retry-run-confirm-v1", fingerprint + "\0" + expiresAt);
        return new RetryConfirmation(sourceRunId, normalized, count, expiresAt, token);
    }

    public CreationResult retry(WorkspaceId workspaceId, String sourceRunId, String idempotencyKey,
                                RetryRun command) {
        String key = ApplicationSupport.text(idempotencyKey, "Idempotency-Key");
        Set<String> states = retryStates(command.states());
        RetryConfirmation confirmation = confirmRetry(workspaceId, sourceRunId, states);
        verifyConfirmation("retry-run-confirm-v1", command.confirmationToken(),
                retryFingerprint(workspaceId, get(workspaceId, sourceRunId), states, confirmation.itemCount()));
        JsonDocument request = json.canonicalize(Map.of("sourceRunId", sourceRunId,
                "states", states, "confirmation", ApplicationSupport.sha256(command.confirmationToken())));
        String keyHash = ApplicationSupport.sha256(key);
        String requestHash = ApplicationSupport.sha256(request.value());
        Created created = transactions.required(() -> createRetryInTransaction(workspaceId, sourceRunId,
                states, keyHash, requestHash, confirmation.itemCount()));
        if (!created.replayed()) {
            events.publish(created.event());
            dispatcher.dispatch(workspaceId, created.run().id());
        }
        return new CreationResult(created.run(), created.replayed());
    }

    public RunDefinition get(WorkspaceId workspaceId, String runId) {
        return runs.findById(workspaceId, runId).orElseThrow(() -> notFound("Run", runId));
    }

    public List<RunDefinition> list(WorkspaceId workspaceId) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        return runs.list(workspaceId);
    }

    public RunSnapshot snapshot(WorkspaceId workspaceId, String runId) {
        return runs.findSnapshot(workspaceId, runId).orElseThrow(() -> notFound("Run snapshot", runId));
    }

    public List<RunEventRecord> eventsAfter(WorkspaceId workspaceId, String runId,
                                            long sequenceExclusive, int limit) {
        get(workspaceId, runId);
        return runs.eventsAfter(workspaceId, runId, sequenceExclusive, limit);
    }

    private Created createInTransaction(WorkspaceId workspaceId, String jobId, String keyHash,
                                        String requestHash, CreateRun command) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        Instant now = clock.instant();
        IdempotencyRecord previous = runs.findIdempotency(workspaceId, IDEMPOTENCY_SCOPE, keyHash).orElse(null);
        if (previous != null && previous.expiresAt().isAfter(now)) {
            if (!previous.requestHash().equals(requestHash)) {
                throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                        "Idempotency-Key was already used for a different request");
            }
            RunDefinition replay = runs.findById(workspaceId, previous.resourceId())
                    .orElseThrow(() -> new IllegalStateException("idempotency record references a missing run"));
            return new Created(replay, null, true);
        }

        JobDefinition job = jobs.findById(workspaceId, jobId).orElseThrow(() -> notFound("Job", jobId));
        if (!job.enabled() || job.archived()) {
            throw new ApplicationProblem(ApplicationProblem.Kind.UNPROCESSABLE, "JOB_DISABLED",
                    "Disabled Job cannot create a Run");
        }
        AccountDefinition account = accounts.findById(workspaceId, job.accountId())
                .orElseThrow(() -> notFound("Account", job.accountId()));
        MessageDefinition message = messages.findById(workspaceId, job.messageId())
                .orElseThrow(() -> notFound("Message", job.messageId()));
        AudienceDefinition audience = audiences.findById(workspaceId, job.audienceId())
                .orElseThrow(() -> notFound("Audience", job.audienceId()));
        if (!account.provider().equals(message.provider())) {
            throw new ApplicationProblem(ApplicationProblem.Kind.UNPROCESSABLE, "PROVIDER_MISMATCH",
                    "Run resources do not use the same Provider version");
        }
        ProviderFactory provider = ApplicationSupport.requireProvider(providers, account.provider());
        ApplicationSupport.requireValid(provider.validateAccount(
                ApplicationSupport.config(account.configuration(), provider.descriptor().accountSchema())));
        ApplicationSupport.requireValid(provider.validateMessage(
                ApplicationSupport.config(message.content(), provider.descriptor().messageSchema())));

        JsonDocument policies = mergedPolicies(job.policies(), command.policyOverrides());
        String runId = ids.next("run");
        String snapshotId = ids.next("runsnap");
        JsonDocument snapshotContent = json.canonicalize(Map.of(
                "providerId", account.provider().providerId(),
                "providerVersion", account.provider().implementationVersion(),
                "account", json.read(account.configuration(), Object.class),
                "message", json.read(message.content(), Object.class),
                "policies", json.read(policies, Object.class),
                "audienceSnapshotId", audience.snapshotId(),
                "audienceContentHash", audience.contentHash()));
        RunSnapshot snapshot = new RunSnapshot(snapshotId, runId, workspaceId, account.provider(),
                account.configuration(), message.content(), policies, audience.snapshotId(),
                ApplicationSupport.sha256(snapshotContent.value()));
        RunDefinition run = new RunDefinition(runId, workspaceId, job.id(), RunStatus.PENDING,
                command.reason() == null ? "manual" : command.reason(), command.dryRun(), null, "",
                audience.recordCount(), 0, 0, 0, 0, 0, 0, now, null, null, now, 0);
        RunEventRecord event = new RunEventRecord(runId, workspaceId, 1, "RUN_CREATED", now,
                json.canonicalize(Map.of("jobId", job.id(), "dryRun", command.dryRun(),
                        "total", audience.recordCount(), "reason", run.stateReason())),
                RunEventRecord.Severity.INFO);
        IdempotencyRecord idempotency = new IdempotencyRecord(workspaceId, IDEMPOTENCY_SCOPE,
                keyHash, requestHash, runId, 202, now, now.plus(Duration.ofHours(24)));
        runs.create(run, snapshot, event, idempotency);
        return new Created(run, event, false);
    }

    @SuppressWarnings("unchecked")
    private JsonDocument mergedPolicies(JsonDocument base, Object overrides) {
        Map<String, Object> merged = new LinkedHashMap<>(json.read(base, Map.class));
        if (overrides != null) {
            JsonDocument canonicalOverrides = json.canonicalize(overrides);
            deepMerge(merged, json.read(canonicalOverrides, Map.class));
        }
        return json.canonicalize(merged);
    }

    private Created createRetryInTransaction(WorkspaceId workspaceId, String sourceRunId,
                                             Set<String> states, String keyHash,
                                             String requestHash, long count) {
        Instant now = clock.instant();
        IdempotencyRecord previous = runs.findIdempotency(workspaceId, "RETRY_RUN", keyHash).orElse(null);
        if (previous != null && previous.expiresAt().isAfter(now)) {
            if (!previous.requestHash().equals(requestHash)) {
                throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                        "Idempotency-Key was already used for a different retry request");
            }
            return new Created(runs.findById(workspaceId, previous.resourceId()).orElseThrow(), null, true);
        }
        RunDefinition source = runs.findById(workspaceId, sourceRunId)
                .orElseThrow(() -> notFound("Run", sourceRunId));
        RunSnapshot sourceSnapshot = runs.findSnapshot(workspaceId, sourceRunId)
                .orElseThrow(() -> notFound("Run snapshot", sourceRunId));
        String runId = ids.next("run");
        String retryStateText = String.join(",", states.stream().sorted().toList());
        RunDefinition run = new RunDefinition(runId, workspaceId, source.jobId(), RunStatus.PENDING,
                "retry:" + sourceRunId, false, sourceRunId, retryStateText,
                count, 0, 0, 0, 0, 0, 0, now, null, null, now, 0);
        RunSnapshot snapshot = new RunSnapshot(ids.next("runsnap"), runId, workspaceId,
                sourceSnapshot.provider(), sourceSnapshot.accountConfiguration(), sourceSnapshot.messageContent(),
                sourceSnapshot.policies(), sourceSnapshot.audienceSnapshotId(), sourceSnapshot.contentHash());
        RunEventRecord event = new RunEventRecord(runId, workspaceId, 1, "RUN_RETRY_CREATED", now,
                json.canonicalize(Map.of("sourceRunId", sourceRunId, "states", states, "total", count)),
                RunEventRecord.Severity.INFO);
        IdempotencyRecord idempotency = new IdempotencyRecord(workspaceId, "RETRY_RUN", keyHash,
                requestHash, runId, 202, now, now.plus(Duration.ofHours(24)));
        runs.createRetry(run, snapshot, event, idempotency, sourceRunId, states);
        return new Created(run, event, false);
    }

    private void requireLiveConfirmation(WorkspaceId workspaceId, String jobId, String token) {
        verifyConfirmation("live-run-confirm-v1", token, confirmationContext(workspaceId, jobId).fingerprint());
    }

    private void verifyConfirmation(String purpose, String token, String expectedFingerprint) {
        if (token == null || token.isBlank()) throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT,
                "LIVE_CONFIRMATION_REQUIRED", "A fresh send confirmation is required");
        try {
            String decoded = cursors.decode(purpose, token);
            int separator = decoded.lastIndexOf('\0');
            if (separator < 1 || !decoded.substring(0, separator).equals(expectedFingerprint)
                    || !Instant.parse(decoded.substring(separator + 1)).isAfter(clock.instant())) {
                throw new IllegalArgumentException("confirmation payload");
            }
        } catch (IllegalArgumentException | DateTimeException problem) {
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "LIVE_CONFIRMATION_STALE",
                    "Send confirmation is invalid, expired, or the resources changed");
        }
    }

    private ConfirmationContext confirmationContext(WorkspaceId workspaceId, String jobId) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        JobDefinition job = jobs.findById(workspaceId, jobId).orElseThrow(() -> notFound("Job", jobId));
        if (!job.enabled() || job.archived()) throw new ApplicationProblem(ApplicationProblem.Kind.UNPROCESSABLE,
                "JOB_DISABLED", "Disabled or archived Job cannot create a Run");
        AccountDefinition account = accounts.findById(workspaceId, job.accountId())
                .orElseThrow(() -> notFound("Account", job.accountId()));
        MessageDefinition message = messages.findById(workspaceId, job.messageId())
                .orElseThrow(() -> notFound("Message", job.messageId()));
        AudienceDefinition audience = audiences.findById(workspaceId, job.audienceId())
                .orElseThrow(() -> notFound("Audience", job.audienceId()));
        String fingerprint = ApplicationSupport.sha256(String.join("\0", workspaceId.value(), job.id(),
                String.valueOf(job.version()), String.valueOf(account.version()), message.contentHash(),
                audience.snapshotId(), job.policies().value()));
        return new ConfirmationContext(job, account, message, audience, fingerprint);
    }

    private static Set<String> retryStates(Set<String> states) {
        Set<String> normalized = new TreeSet<>();
        if (states != null) states.forEach(value -> normalized.add(value == null ? "" : value.toUpperCase()));
        Set<String> allowed = Set.of("FAILED", "UNKNOWN", "UNSENT");
        if (normalized.isEmpty() || !allowed.containsAll(normalized)) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "INVALID_RETRY_STATES",
                    "Retry states must contain FAILED, UNKNOWN, or UNSENT");
        }
        return Set.copyOf(normalized);
    }

    private static String retryFingerprint(WorkspaceId workspaceId, RunDefinition source,
                                           Set<String> states, long count) {
        return ApplicationSupport.sha256(String.join("\0", workspaceId.value(), source.id(),
                String.valueOf(source.version()), String.join(",", states.stream().sorted().toList()),
                String.valueOf(count)));
    }

    private static Map<?, ?> section(Map<?, ?> root, String name) {
        Object value = root.get(name); return value instanceof Map<?, ?> map ? map : Map.of();
    }
    private static int integer(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }
    private static long longValue(Object value, long fallback) {
        return value instanceof Number number ? number.longValue() : fallback;
    }

    @SuppressWarnings("unchecked")
    private static void deepMerge(Map<String, Object> target, Map<String, Object> overrides) {
        overrides.forEach((key, value) -> {
            Object current = target.get(key);
            if (current instanceof Map<?, ?> currentMap && value instanceof Map<?, ?> overrideMap) {
                deepMerge((Map<String, Object>) currentMap, (Map<String, Object>) overrideMap);
            } else {
                target.put(key, value);
            }
        });
    }

    private static ApplicationProblem notFound(String type, String id) {
        return new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND,
                type.toUpperCase().replace(' ', '_') + "_NOT_FOUND", type + " was not found: " + id);
    }

    public record CreateRun(boolean dryRun, Object policyOverrides, String reason, String confirmationToken) {
    }

    public record LiveConfirmation(String jobId, String jobName, String providerId, String providerVersion,
                                   String accountId, String accountName, String audienceId, String audienceName,
                                   long audienceCount, Object policies, int targetConcurrency,
                                   long rateLimitPermits, String rateLimitPeriod, long estimatedItems,
                                   Instant expiresAt, String confirmationToken) { }
    public record RetryConfirmation(String sourceRunId, Set<String> states, long itemCount,
                                    Instant expiresAt, String confirmationToken) { }
    public record RetryRun(Set<String> states, String confirmationToken) { }

    public record CreationResult(RunDefinition run, boolean replayed) {
    }

    private record Created(RunDefinition run, RunEventRecord event, boolean replayed) {
    }

    private record ConfirmationContext(JobDefinition job, AccountDefinition account,
                                       MessageDefinition message, AudienceDefinition audience,
                                       String fingerprint) { }
}
