package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import com.fangxuele.wepush.next.agent.protocol.RemoteRunDocuments;
import com.fangxuele.wepush.next.agent.protocol.SecretEnvelopeCodec;
import com.fangxuele.wepush.next.core.api.CommandResult;
import com.fangxuele.wepush.next.core.api.ItemState;
import com.fangxuele.wepush.next.core.api.RunCommand;
import com.fangxuele.wepush.next.core.api.SecretRef;
import com.fangxuele.wepush.next.core.api.SecretValue;
import com.fangxuele.wepush.next.service.domain.AgentLease;
import com.fangxuele.wepush.next.service.domain.AgentLeaseRepository;
import com.fangxuele.wepush.next.service.domain.AgentOutboundMessage;
import com.fangxuele.wepush.next.service.domain.AgentOutboundMessageRepository;
import com.fangxuele.wepush.next.service.domain.AgentRegistration;
import com.fangxuele.wepush.next.service.domain.AgentRepository;
import com.fangxuele.wepush.next.service.domain.AudienceRecipient;
import com.fangxuele.wepush.next.service.domain.AudienceRepository;
import com.fangxuele.wepush.next.service.domain.ArtifactDefinition;
import com.fangxuele.wepush.next.service.domain.ArtifactRepository;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.RunDefinition;
import com.fangxuele.wepush.next.service.domain.RunEventRecord;
import com.fangxuele.wepush.next.service.domain.RunItemResultRecord;
import com.fangxuele.wepush.next.service.domain.RunRepository;
import com.fangxuele.wepush.next.service.domain.RunResultRepository;
import com.fangxuele.wepush.next.service.domain.RunSnapshot;
import com.fangxuele.wepush.next.service.domain.RunStatus;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Durable remote-run scheduler and the receiving side of the Agent execution protocol. */
public final class RemoteRunCoordinator implements RunDispatcher, RunCommandGateway {
    private final WorkspaceRepository workspaces;
    private final RunRepository runs;
    private final RunResultRepository results;
    private final AudienceRepository audiences;
    private final AgentRepository agents;
    private final AgentIdentityService agentIdentities;
    private final AgentLeaseRepository leases;
    private final AgentOutboundMessageRepository outbound;
    private final ArtifactRepository artifacts;
    private final AgentControlGateway gateway;
    private final SecretStore secrets;
    private final JsonCodec json;
    private final SecretReferenceScanner secretReferences;
    private final SecretEnvelopeCodec secretEnvelopes = new SecretEnvelopeCodec();
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final RunEventPublisher eventPublisher;
    private final Clock clock;
    private final String publicBaseUrl;
    private final Duration offerTtl;
    private final Duration recoveryGrace;

    public RemoteRunCoordinator(
            RunRepository runs,
            WorkspaceRepository workspaces,
            RunResultRepository results,
            AudienceRepository audiences,
            AgentRepository agents,
            AgentIdentityService agentIdentities,
            AgentLeaseRepository leases,
            AgentOutboundMessageRepository outbound,
            ArtifactRepository artifacts,
            AgentControlGateway gateway,
            SecretStore secrets,
            JsonCodec json,
            ResourceIdGenerator ids,
            TransactionRunner transactions,
            RunEventPublisher eventPublisher,
            Clock clock,
            String publicBaseUrl,
            Duration offerTtl,
            Duration recoveryGrace
    ) {
        this.runs = Objects.requireNonNull(runs, "runs");
        this.workspaces = Objects.requireNonNull(workspaces, "workspaces");
        this.results = Objects.requireNonNull(results, "results");
        this.audiences = Objects.requireNonNull(audiences, "audiences");
        this.agents = Objects.requireNonNull(agents, "agents");
        this.agentIdentities = Objects.requireNonNull(agentIdentities, "agentIdentities");
        this.leases = Objects.requireNonNull(leases, "leases");
        this.outbound = Objects.requireNonNull(outbound, "outbound");
        this.artifacts = Objects.requireNonNull(artifacts, "artifacts");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.secrets = Objects.requireNonNull(secrets, "secrets");
        this.json = Objects.requireNonNull(json, "json");
        this.secretReferences = new SecretReferenceScanner(json);
        this.ids = Objects.requireNonNull(ids, "ids");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalArgumentException("Agent public base URL is required");
        }
        this.publicBaseUrl = publicBaseUrl.replaceFirst("/+$", "");
        if (offerTtl == null || offerTtl.isNegative() || offerTtl.isZero()) {
            throw new IllegalArgumentException("Agent lease offer TTL must be positive");
        }
        this.offerTtl = offerTtl;
        if (recoveryGrace == null || recoveryGrace.isNegative()) {
            throw new IllegalArgumentException("Agent recovery grace must not be negative");
        }
        this.recoveryGrace = recoveryGrace;
    }

    @Override
    public void dispatch(WorkspaceId workspaceId, String runId) {
        RunDefinition run = runs.findById(workspaceId, runId).orElse(null);
        if (run == null || (run.status() != RunStatus.PENDING && run.status() != RunStatus.RECOVERING)) {
            return;
        }
        AgentLease latestLease = leases.findCurrent(workspaceId, runId).orElse(null);
        if (run.status() == RunStatus.RECOVERING && latestLease != null
                && latestLease.status() == AgentLease.Status.LOST
                && latestLease.acknowledgedAt() != null
                && latestLease.completedAt() != null
                && latestLease.completedAt().plus(recoveryGrace).isAfter(clock.instant())) {
            return;
        }
        RunSnapshot snapshot = runs.findSnapshot(workspaceId, runId)
                .orElseThrow(() -> new IllegalStateException("run snapshot is missing: " + runId));
        List<SecretRef> requiredSecrets = secretReferences.scan(
                snapshot.accountConfiguration(), snapshot.messageContent());
        AgentRegistration agent = chooseAgent(snapshot, !requiredSecrets.isEmpty()).orElse(null);
        if (agent == null) return;

        Instant now = clock.instant();
        long epoch = leases.nextEpoch(workspaceId, runId);
        AgentLease lease = new AgentLease(ids.next("lease"), workspaceId, runId, agent.id(),
                agent.sessionId(), epoch, ids.next("fence"), AgentLease.Status.OFFERED,
                now, now.plus(offerTtl), null, null, 0, 0);
        LeaseFence fence = fence(lease);
        AgentOutboundMessage message = new AgentOutboundMessage("offer_" + lease.id(),
                workspaceId, runId, agent.id(), lease.id(), AgentOutboundMessage.Type.LEASE_OFFER,
                "", new JsonDocument("{}"), now, now, null, null, 0, "");

        RunEventRecord offered = transactions.required(() -> {
            AgentLease current = leases.findCurrent(workspaceId, runId).orElse(null);
            if (current != null && current.status().active()) return null;
            RunDefinition latest = runs.findById(workspaceId, runId).orElse(null);
            if (latest == null || (latest.status() != RunStatus.PENDING
                    && latest.status() != RunStatus.RECOVERING)) return null;
            leases.create(lease);
            outbound.create(message);
            if (!runs.transition(workspaceId, runId,
                    Set.of(RunStatus.PENDING, RunStatus.RECOVERING), RunStatus.LEASED,
                    "offered to Agent " + agent.id(), now)) {
                throw new IllegalStateException("run could not enter LEASED state: " + runId);
            }
            RunEventRecord event = event(lease, "RUN_LEASE_OFFERED", now,
                    Map.of("agentId", agent.id(), "leaseId", lease.id(), "epoch", epoch),
                    RunEventRecord.Severity.INFO);
            runs.appendEvent(event);
            return event;
        });
        if (offered == null) return;
        eventPublisher.publish(offered);
        deliver(message);
    }

    public Optional<AgentFrames.ServicePayload> accept(AgentRegistration agent,
                                                       AgentFrames.AgentPayload payload) {
        if (payload instanceof AgentFrames.LeaseAck acknowledgement) {
            acknowledge(agent, acknowledgement.fence());
        } else if (payload instanceof AgentFrames.EventBatch batch) {
            return Optional.of(acceptEvents(agent, batch));
        } else if (payload instanceof AgentFrames.RunCompleted completed) {
            complete(agent, completed);
            return Optional.of(new AgentFrames.RunCompletionAck(completed.fence()));
        } else if (payload instanceof AgentFrames.CommandAck commandAck) {
            validate(agent, commandAck.fence());
            outbound.acknowledgeCommand(commandAck.commandId(), clock.instant());
        } else if (payload instanceof AgentFrames.Heartbeat heartbeat) {
            renew(agent, heartbeat.leases());
        }
        return Optional.empty();
    }

    public List<LeaseFence> reconnected(AgentRegistration agent, List<LeaseFence> recovered) {
        if (recovered.size() > agent.maximumRuns()) {
            throw new RemoteProtocolProblem("RECOVERED_LEASE_CAPACITY_INVALID",
                    "Agent reported more recovered Leases than its capacity");
        }
        if (recovered.stream().map(LeaseFence::leaseId).distinct().count() != recovered.size()) {
            throw new RemoteProtocolProblem("RECOVERED_LEASE_DUPLICATE",
                    "Agent reported duplicate recovered Leases");
        }
        Instant now = clock.instant();
        Instant renewedUntil = now.plus(offerTtl);
        return transactions.required(() -> recovered.stream()
                .filter(fence -> leases.renew(authority(fence), agent.id(), agent.sessionId(),
                        now, renewedUntil))
                .toList());
    }

    @Override
    public CommandResult submit(WorkspaceId workspaceId, String runId, RunCommand command) {
        AgentLease lease = leases.findCurrent(workspaceId, runId).orElse(null);
        if (lease == null || (lease.status() != AgentLease.Status.ACKNOWLEDGED
                && lease.status() != AgentLease.Status.RUNNING)) {
            return CommandResult.rejected(command.commandId(), "REMOTE_RUN_NOT_ACTIVE",
                    "Run has no active Agent lease");
        }
        CommandDocument document = command(command);
        Instant now = clock.instant();
        AgentOutboundMessage message = new AgentOutboundMessage(command.commandId(), workspaceId,
                runId, lease.agentId(), lease.id(), AgentOutboundMessage.Type.RUN_COMMAND,
                document.type(), new JsonDocument(new String(document.payload(), StandardCharsets.UTF_8)),
                now, now, null, null, 0, "");
        transactions.required(() -> outbound.create(message));
        return deliver(message)
                ? CommandResult.accepted(command.commandId(), "REMOTE_COMMAND_DELIVERED")
                : CommandResult.accepted(command.commandId(), "REMOTE_COMMAND_QUEUED");
    }

    /** Retries durable messages on every instance; only the stream-owning instance can send. */
    public void deliverPending() {
        outbound.pending(clock.instant(), 100).forEach(this::deliver);
    }

    /** Gives a newly connected Agent an immediate delivery pass instead of waiting for the poller. */
    public void deliverPendingForAgent(String agentId) {
        outbound.pendingForAgent(agentId, clock.instant(), 100).forEach(this::deliver);
    }

    public byte[] executionSpec(String leaseId) {
        AgentLease lease = requireDownloadable(leaseId);
        RunDefinition run = runs.findById(lease.workspaceId(), lease.runId())
                .orElseThrow(() -> new RemoteProtocolProblem("RUN_NOT_FOUND", "leased run is missing"));
        RunSnapshot snapshot = runs.findSnapshot(lease.workspaceId(), lease.runId())
                .orElseThrow(() -> new RemoteProtocolProblem("SNAPSHOT_NOT_FOUND", "run snapshot is missing"));
        return executionSpecDocument(lease, snapshot, run);
    }

    public byte[] audience(String leaseId) {
        AgentLease lease = requireDownloadable(leaseId);
        RunSnapshot snapshot = runs.findSnapshot(lease.workspaceId(), lease.runId())
                .orElseThrow(() -> new RemoteProtocolProblem("SNAPSHOT_NOT_FOUND", "run snapshot is missing"));
        return audienceDocument(lease, snapshot);
    }

    public void recoverPending() {
        workspaces.list().forEach(workspace -> runs.list(workspace.id()).stream()
                .filter(run -> run.status() == RunStatus.PENDING || run.status() == RunStatus.RECOVERING)
                .forEach(run -> dispatch(workspace.id(), run.id())));
    }

    public void expireAndRecover() {
        for (AgentLease lease : transactions.required(() -> leases.expireActive(clock.instant()))) {
            recoverLease(lease, "LEASE_EXPIRED");
        }
        recoverPending();
    }

    public void disconnected(AgentRegistration agent) {
        // A transport disconnect is not proof that execution stopped. The Lease remains
        // fenced and may be rebound by the same Agent until its database expiry.
    }

    private void renew(AgentRegistration agent, List<LeaseFence> reported) {
        Instant now = clock.instant();
        Instant renewedUntil = now.plus(offerTtl);
        transactions.required(() -> {
            for (LeaseFence fence : reported) {
                leases.renew(authority(fence), agent.id(), agent.sessionId(), now, renewedUntil);
            }
        });
    }

    private Optional<AgentRegistration> chooseAgent(RunSnapshot snapshot, boolean requiresSecrets) {
        return agents.list().stream()
                .filter(agent -> agent.status() == AgentRegistration.Status.ONLINE)
                .filter(agent -> agentIdentities.allowedInWorkspace(agent.id(), snapshot.workspaceId()))
                .filter(agent -> !requiresSecrets || !agent.secretEncryptionPublicKey().isBlank())
                .filter(agent -> agent.availableRuns()
                        > leases.offeredCount(agent.id(), agent.sessionId()))
                .filter(agent -> agent.providers().stream().anyMatch(provider ->
                        provider.providerId().equals(snapshot.provider().providerId())
                                && provider.implementationVersion().equals(
                                snapshot.provider().implementationVersion())))
                .sorted(Comparator.comparingInt(AgentRegistration::availableRuns).reversed()
                        .thenComparing(AgentRegistration::id))
                .findFirst();
    }

    private byte[] secretEnvelope(AgentRegistration agent, AgentLease lease,
                                  List<SecretRef> required) {
        if (required.isEmpty()) return new byte[0];
        List<SecretEnvelopeCodec.SecretMaterial> materials = new ArrayList<>(required.size());
        try {
            for (SecretRef ref : required) {
                try (SecretValue value = secrets.resolve(lease.workspaceId(), ref)) {
                    byte[] clear = value.copyBytes();
                    try {
                        materials.add(new SecretEnvelopeCodec.SecretMaterial(
                                ref.namespace(), ref.name(), ref.version(), clear));
                    } finally {
                        Arrays.fill(clear, (byte) 0);
                    }
                }
            }
            return secretEnvelopes.seal(agent.id(), fence(lease), lease.expiresAt(),
                    agent.secretEncryptionPublicKey(), materials);
        } finally {
            materials.forEach(SecretEnvelopeCodec.SecretMaterial::close);
        }
    }

    private void acknowledge(AgentRegistration agent, LeaseFence fence) {
        AgentLease lease = validate(agent, fence);
        if (lease.status() == AgentLease.Status.ACKNOWLEDGED
                || lease.status() == AgentLease.Status.RUNNING) return;
        if (lease.status() != AgentLease.Status.OFFERED) {
            throw new RemoteProtocolProblem("LEASE_NOT_OFFERED", "lease cannot be acknowledged");
        }
        Instant now = clock.instant();
        RunEventRecord accepted = transactions.required(() -> {
            if (!leases.acknowledge(lease.id(), agent.id(), agent.sessionId(),
                    lease.fencingToken(), now)) {
                throw new RemoteProtocolProblem("LEASE_ACK_REJECTED", "lease acknowledgement is stale");
            }
            outbound.acknowledgeLease(lease.id(), now);
            runs.transition(lease.workspaceId(), lease.runId(), Set.of(RunStatus.LEASED),
                    RunStatus.RUNNING, "executing on Agent " + agent.id(), now);
            RunEventRecord event = event(lease, "RUN_LEASE_ACCEPTED", now,
                    Map.of("agentId", agent.id(), "leaseId", lease.id(), "epoch", lease.epoch()),
                    RunEventRecord.Severity.INFO);
            runs.appendEvent(event);
            return event;
        });
        eventPublisher.publish(accepted);
    }

    private AgentFrames.EventAck acceptEvents(AgentRegistration agent, AgentFrames.EventBatch batch) {
        AgentLease lease = validate(agent, batch.fence());
        if (batch.events().isEmpty()) {
            throw new RemoteProtocolProblem("EVENT_BATCH_EMPTY", "event batch must not be empty");
        }
        long previous = lease.lastEventSequence();
        if (batch.firstEventSequence() > previous + 1) {
            throw new RemoteProtocolProblem("EVENT_SEQUENCE_GAP",
                    "expected Agent event sequence " + (previous + 1));
        }
        long batchLast = batch.firstEventSequence() + batch.events().size() - 1L;
        if (batchLast <= previous) return new AgentFrames.EventAck(batch.fence(), previous);
        int skip = Math.toIntExact(Math.max(0, previous - batch.firstEventSequence() + 1));
        List<RemoteRunDocuments.Report> reports = batch.events().subList(skip, batch.events().size())
                .stream().map(this::report).toList();
        List<RunEventRecord> published = transactions.required(() -> {
            List<RunEventRecord> recorded = new ArrayList<>();
            for (RemoteRunDocuments.Report report : reports) {
                if ("EVENT".equals(report.kind())) {
                    RemoteRunDocuments.Event source = report.event();
                    requireRun(lease, source.runId());
                    if ("RUN_STARTED".equals(source.type())) {
                        leases.markRunning(lease.id(), lease.fencingToken());
                    }
                    RunEventRecord event = event(lease, source.type(), source.occurredAt(), source.data(),
                            "RUN_FAILED".equals(source.type())
                                    ? RunEventRecord.Severity.ERROR : RunEventRecord.Severity.INFO);
                    runs.appendEvent(event);
                    recorded.add(event);
                } else {
                    List<RunItemResultRecord> values = report.results().stream().map(source -> {
                        requireRun(lease, source.runId());
                        return new RunItemResultRecord(source.runId(), lease.workspaceId(), source.itemId(),
                                source.attempts(), ItemState.valueOf(source.state()), source.providerCode(),
                                source.diagnostic(), source.externalRequestId(), source.completedAt(),
                                json.canonicalize(source.metadata()));
                    }).toList();
                    results.append(values);
                }
            }
            if (!leases.advanceEvents(lease.id(), lease.fencingToken(), previous, batchLast)) {
                throw new RemoteProtocolProblem("EVENT_SEQUENCE_CONFLICT",
                        "Agent event cursor changed concurrently");
            }
            return recorded;
        });
        published.forEach(eventPublisher::publish);
        return new AgentFrames.EventAck(batch.fence(), batchLast);
    }

    private void complete(AgentRegistration agent, AgentFrames.RunCompleted completed) {
        AgentLease lease = validate(agent, completed.fence());
        if (lease.status() == AgentLease.Status.COMPLETED) return;
        validateArtifacts(lease, completed.artifactReferences());
        RemoteRunDocuments.Summary summary = json.read(
                new JsonDocument(new String(completed.summary(), StandardCharsets.UTF_8)),
                RemoteRunDocuments.Summary.class);
        requireRun(lease, summary.runId());
        RunStatus status = terminalStatus(summary.finalState());
        RunEventRecord finalized = transactions.required(() -> {
            if (!leases.complete(lease.id(), lease.fencingToken(), summary.endedAt())) {
                throw new RemoteProtocolProblem("LEASE_COMPLETION_REJECTED", "lease completion is stale");
            }
            runs.complete(lease.workspaceId(), lease.runId(), status,
                    "completed on Agent " + agent.id(), summary.total(), summary.succeeded(),
                    summary.failed(), summary.unknown(), summary.unsent(), summary.skipped(),
                    summary.retried(), summary.endedAt());
            RunEventRecord event = event(lease, "RUN_FINALIZED", summary.endedAt(),
                    Map.of("state", status.name(), "agentId", agent.id(),
                            "total", summary.total(), "succeeded", summary.succeeded(),
                            "failed", summary.failed(), "unknown", summary.unknown(),
                            "unsent", summary.unsent(), "skipped", summary.skipped(),
                            "retried", summary.retried()),
                    status == RunStatus.SUCCEEDED ? RunEventRecord.Severity.INFO
                            : RunEventRecord.Severity.WARNING);
            runs.appendEvent(event);
            return event;
        });
        eventPublisher.publish(finalized);
    }

    private void validateArtifacts(AgentLease lease, List<String> references) {
        if (references.size() > 1_000 || references.stream().distinct().count() != references.size()) {
            throw new RemoteProtocolProblem("ARTIFACT_REFERENCES_INVALID",
                    "Run completion Artifact references are duplicated or excessive");
        }
        for (String artifactId : references) {
            ArtifactDefinition artifact = artifacts.findById(lease.workspaceId(), artifactId).orElseThrow(() ->
                    new RemoteProtocolProblem("ARTIFACT_NOT_FOUND", "Run Artifact is unknown"));
            if (!lease.runId().equals(artifact.runId()) || artifact.state() != ArtifactDefinition.State.READY) {
                throw new RemoteProtocolProblem("ARTIFACT_NOT_READY",
                        "Run Artifact is not READY for this Lease");
            }
        }
    }

    private AgentLease validate(AgentRegistration agent, LeaseFence fence) {
        AgentLease lease = leases.findById(fence.leaseId()).orElseThrow(() ->
                new RemoteProtocolProblem("LEASE_UNKNOWN", "lease is unknown"));
        if (!lease.runId().equals(fence.runId()) || lease.epoch() != fence.epoch()
                || !lease.fencingToken().equals(fence.fencingToken())
                || !lease.agentId().equals(agent.id())
                || !lease.agentSessionId().equals(agent.sessionId())) {
            throw new RemoteProtocolProblem("LEASE_FENCE_STALE", "lease fencing authority is stale");
        }
        return lease;
    }

    private AgentLease requireDownloadable(String leaseId) {
        AgentLease lease = leases.findById(leaseId).orElseThrow(() ->
                new RemoteProtocolProblem("LEASE_UNKNOWN", "lease is unknown"));
        if (!lease.status().active() || !lease.expiresAt().isAfter(clock.instant())) {
            throw new RemoteProtocolProblem("LEASE_NOT_DOWNLOADABLE", "lease is inactive or expired");
        }
        return lease;
    }

    private byte[] executionSpecDocument(AgentLease lease, RunSnapshot snapshot, RunDefinition run) {
        RemoteRunDocuments.ExecutionSpec document = new RemoteRunDocuments.ExecutionSpec(
                lease.runId(), lease.workspaceId().value(), snapshot.provider().providerId(),
                snapshot.provider().implementationVersion(), snapshot.accountConfiguration().value(),
                snapshot.messageContent().value(), snapshot.policies().value(),
                Map.of("workspaceId", lease.workspaceId().value(), "runSnapshotId", snapshot.id(),
                        "leaseId", lease.id(), "leaseEpoch", Long.toString(lease.epoch())),
                run.dryRun(), run.createdAt());
        return json.canonicalize(document).value().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] audienceDocument(AgentLease lease, RunSnapshot snapshot) {
        List<RemoteRunDocuments.Recipient> recipients = audiences.recipientsForRun(
                        lease.workspaceId(), snapshot.audienceSnapshotId(), lease.runId()).stream()
                .map(this::recipient).toList();
        return json.canonicalize(new RemoteRunDocuments.Audience(recipients))
                .value().getBytes(StandardCharsets.UTF_8);
    }

    private RemoteRunDocuments.Recipient recipient(AudienceRecipient value) {
        return new RemoteRunDocuments.Recipient(value.sequence(), value.itemId(), value.fields().value());
    }

    private RemoteRunDocuments.Report report(byte[] bytes) {
        return json.read(new JsonDocument(new String(bytes, StandardCharsets.UTF_8)),
                RemoteRunDocuments.Report.class);
    }

    private void lose(AgentLease lease, String reason, boolean redispatch) {
        LeaseLoss outcome = transactions.required(() -> {
            if (!leases.markLost(lease.id(), lease.fencingToken(), clock.instant())) {
                return new LeaseLoss(false, null);
            }
            outbound.discardLease(lease.id(), reason, clock.instant());
            RunDefinition run = runs.findById(lease.workspaceId(), lease.runId()).orElse(null);
            if (run == null || run.status().terminal()) return new LeaseLoss(true, null);
            RunStatus target = run.status() == RunStatus.CANCELLING ? RunStatus.LOST : RunStatus.RECOVERING;
            runs.transition(lease.workspaceId(), lease.runId(), Set.of(run.status()), target, reason,
                    clock.instant());
            RunEventRecord event = event(lease, "RUN_LEASE_LOST", clock.instant(),
                    Map.of("agentId", lease.agentId(), "leaseId", lease.id(), "code", reason),
                    RunEventRecord.Severity.WARNING);
            runs.appendEvent(event);
            return new LeaseLoss(true, event);
        });
        if (outcome.event() != null) eventPublisher.publish(outcome.event());
        if (outcome.changed() && redispatch) dispatch(lease.workspaceId(), lease.runId());
    }

    private void recoverLease(AgentLease lease, String reason) {
        RunEventRecord recovered = transactions.required(() -> {
            outbound.discardLease(lease.id(), reason, clock.instant());
            RunDefinition run = runs.findById(lease.workspaceId(), lease.runId()).orElse(null);
            if (run != null && !run.status().terminal()) {
                runs.transition(lease.workspaceId(), lease.runId(), Set.of(run.status()),
                        RunStatus.RECOVERING, reason, clock.instant());
                RunEventRecord event = event(lease, "RUN_LEASE_EXPIRED", clock.instant(),
                        Map.of("agentId", lease.agentId(), "leaseId", lease.id()),
                        RunEventRecord.Severity.WARNING);
                runs.appendEvent(event);
                return event;
            }
            return null;
        });
        if (recovered != null) eventPublisher.publish(recovered);
    }

    private RunEventRecord event(AgentLease lease, String type, Instant occurredAt,
                                 Object payload, RunEventRecord.Severity severity) {
        return new RunEventRecord(lease.runId(), lease.workspaceId(),
                runs.nextEventSequence(lease.workspaceId(), lease.runId()), type, occurredAt,
                json.canonicalize(payload), severity);
    }

    private CommandDocument command(RunCommand command) {
        if (command instanceof RunCommand.PauseRun) return new CommandDocument("PAUSE", bytes(Map.of()));
        if (command instanceof RunCommand.ResumeRun) return new CommandDocument("RESUME", bytes(Map.of()));
        if (command instanceof RunCommand.CancelRun cancel) {
            return new CommandDocument("CANCEL", bytes(Map.of("reason", cancel.reason())));
        }
        if (command instanceof RunCommand.ChangeConcurrency concurrency) {
            return new CommandDocument("CONCURRENCY", bytes(Map.of("target", concurrency.target())));
        }
        throw new IllegalArgumentException("unsupported run command: " + command.getClass().getName());
    }

    private byte[] bytes(Object value) {
        return json.canonicalize(value).value().getBytes(StandardCharsets.UTF_8);
    }

    private boolean deliver(AgentOutboundMessage message) {
        Instant now = clock.instant();
        try {
            AgentLease lease = leases.findById(message.leaseId()).orElse(null);
            if (lease == null || !lease.status().active() || !lease.expiresAt().isAfter(now)
                    || !lease.agentId().equals(message.agentId())) {
                outbound.discardLease(message.leaseId(), "LEASE_NO_LONGER_ACTIVE", now);
                return false;
            }
            AgentFrames.ServicePayload payload = switch (message.type()) {
                case LEASE_OFFER -> leaseOffer(lease);
                case RUN_COMMAND -> new AgentFrames.RunCommand(message.id(), fence(lease),
                        message.commandType(), message.payload().value().getBytes(StandardCharsets.UTF_8), now);
            };
            if (gateway.send(message.agentId(), payload)) {
                outbound.delivered(message.id(), now, now.plusSeconds(5));
                return true;
            }
            outbound.failed(message.id(), "AGENT_STREAM_UNAVAILABLE", now.plusSeconds(1));
            return false;
        } catch (RuntimeException problem) {
            outbound.failed(message.id(), problem.getClass().getSimpleName() + ": "
                    + Objects.toString(problem.getMessage(), "delivery failed"), now.plusSeconds(5));
            return false;
        }
    }

    private AgentFrames.LeaseOffer leaseOffer(AgentLease lease) {
        RunDefinition run = runs.findById(lease.workspaceId(), lease.runId())
                .orElseThrow(() -> new IllegalStateException("leased run is missing: " + lease.runId()));
        RunSnapshot snapshot = runs.findSnapshot(lease.workspaceId(), lease.runId())
                .orElseThrow(() -> new IllegalStateException("run snapshot is missing: " + lease.runId()));
        AgentRegistration agent = agents.findById(lease.agentId())
                .orElseThrow(() -> new IllegalStateException("leased Agent is missing: " + lease.agentId()));
        byte[] spec = executionSpecDocument(lease, snapshot, run);
        byte[] audience = audienceDocument(lease, snapshot);
        List<SecretRef> requiredSecrets = secretReferences.scan(
                snapshot.accountConfiguration(), snapshot.messageContent());
        byte[] secretEnvelope = secretEnvelope(agent, lease, requiredSecrets);
        String leasePath = "/internal/agent/v1/leases/"
                + URLEncoder.encode(lease.id(), StandardCharsets.UTF_8);
        return new AgentFrames.LeaseOffer(fence(lease), lease.expiresAt(),
                publicBaseUrl + leasePath + "/execution-spec", sha256(spec),
                publicBaseUrl + leasePath + "/audience", sha256(audience), secretEnvelope);
    }

    private static LeaseFence fence(AgentLease lease) {
        return new LeaseFence(lease.id(), lease.runId(), lease.epoch(), lease.fencingToken());
    }

    private static AgentLeaseRepository.LeaseFenceAuthority authority(LeaseFence fence) {
        return new AgentLeaseRepository.LeaseFenceAuthority(fence.leaseId(), fence.runId(),
                fence.epoch(), fence.fencingToken());
    }

    private static void requireRun(AgentLease lease, String runId) {
        if (!lease.runId().equals(runId)) {
            throw new RemoteProtocolProblem("RUN_ID_MISMATCH", "payload belongs to another run");
        }
    }

    private static RunStatus terminalStatus(String value) {
        return switch (value) {
            case "CANCELLED" -> RunStatus.CANCELLED;
            case "SUCCEEDED" -> RunStatus.SUCCEEDED;
            case "PARTIAL" -> RunStatus.PARTIAL;
            case "FAILED" -> RunStatus.FAILED;
            default -> throw new RemoteProtocolProblem("SUMMARY_STATE_INVALID",
                    "Agent summary is not terminal: " + value);
        };
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private record CommandDocument(String type, byte[] payload) {
    }

    private record LeaseLoss(boolean changed, RunEventRecord event) {
    }

    public static final class RemoteProtocolProblem extends RuntimeException {
        private final String code;

        public RemoteProtocolProblem(String code, String message) {
            super(message);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}
