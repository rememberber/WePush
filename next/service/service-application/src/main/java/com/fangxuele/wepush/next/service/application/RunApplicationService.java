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

public final class RunApplicationService {
    private static final String IDEMPOTENCY_SCOPE = "CREATE_RUN";

    private final WorkspaceRepository workspaces;
    private final AccountRepository accounts;
    private final MessageRepository messages;
    private final AudienceRepository audiences;
    private final JobRepository jobs;
    private final RunRepository runs;
    private final ProviderRegistry providers;
    private final JsonCodec json;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final RunEventPublisher events;
    private final RunDispatcher dispatcher;
    private final Clock clock;

    public RunApplicationService(WorkspaceRepository workspaces, AccountRepository accounts,
                                 MessageRepository messages, AudienceRepository audiences,
                                 JobRepository jobs, RunRepository runs, ProviderRegistry providers,
                                 JsonCodec json, ResourceIdGenerator ids, TransactionRunner transactions,
                                 RunEventPublisher events, RunDispatcher dispatcher, Clock clock) {
        this.workspaces = workspaces;
        this.accounts = accounts;
        this.messages = messages;
        this.audiences = audiences;
        this.jobs = jobs;
        this.runs = runs;
        this.providers = providers;
        this.json = json;
        this.ids = ids;
        this.transactions = transactions;
        this.events = events;
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    public CreationResult create(WorkspaceId workspaceId, String jobId, String idempotencyKey,
                                 CreateRun command) {
        String key = ApplicationSupport.text(idempotencyKey, "Idempotency-Key");
        JsonDocument request = json.canonicalize(Map.of(
                "jobId", ApplicationSupport.text(jobId, "jobId"),
                "dryRun", command.dryRun(),
                "policyOverrides", command.policyOverrides() == null ? Map.of() : command.policyOverrides(),
                "reason", command.reason() == null ? "manual" : command.reason()));
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
        if (!job.enabled()) {
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
                command.reason() == null ? "manual" : command.reason(), command.dryRun(),
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

    public record CreateRun(boolean dryRun, Object policyOverrides, String reason) {
    }

    public record CreationResult(RunDefinition run, boolean replayed) {
    }

    private record Created(RunDefinition run, RunEventRecord event, boolean replayed) {
    }
}
