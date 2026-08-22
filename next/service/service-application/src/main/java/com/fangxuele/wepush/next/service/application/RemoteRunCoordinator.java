package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import com.fangxuele.wepush.next.agent.protocol.RemoteRunDocuments;
import com.fangxuele.wepush.next.core.api.CommandResult;
import com.fangxuele.wepush.next.core.api.ItemState;
import com.fangxuele.wepush.next.core.api.RunCommand;
import com.fangxuele.wepush.next.service.domain.AgentLease;
import com.fangxuele.wepush.next.service.domain.AgentLeaseRepository;
import com.fangxuele.wepush.next.service.domain.AgentRegistration;
import com.fangxuele.wepush.next.service.domain.AgentRepository;
import com.fangxuele.wepush.next.service.domain.AudienceRecipient;
import com.fangxuele.wepush.next.service.domain.AudienceRepository;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.RunDefinition;
import com.fangxuele.wepush.next.service.domain.RunEventRecord;
import com.fangxuele.wepush.next.service.domain.RunItemResultRecord;
import com.fangxuele.wepush.next.service.domain.RunRepository;
import com.fangxuele.wepush.next.service.domain.RunResultRepository;
import com.fangxuele.wepush.next.service.domain.RunSnapshot;
import com.fangxuele.wepush.next.service.domain.RunStatus;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Durable remote-run scheduler and the receiving side of the Agent execution protocol. */
public final class RemoteRunCoordinator implements RunDispatcher, RunCommandGateway {
    private static final WorkspaceId DEFAULT_WORKSPACE = new WorkspaceId("ws_default");

    private final RunRepository runs;
    private final RunResultRepository results;
    private final AudienceRepository audiences;
    private final AgentRepository agents;
    private final AgentLeaseRepository leases;
    private final AgentControlGateway gateway;
    private final JsonCodec json;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final RunEventPublisher eventPublisher;
    private final Clock clock;
    private final String publicBaseUrl;
    private final Duration offerTtl;
    private final Duration recoveryGrace;

    public RemoteRunCoordinator(
            RunRepository runs,
            RunResultRepository results,
            AudienceRepository audiences,
            AgentRepository agents,
            AgentLeaseRepository leases,
            AgentControlGateway gateway,
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
        this.results = Objects.requireNonNull(results, "results");
        this.audiences = Objects.requireNonNull(audiences, "audiences");
        this.agents = Objects.requireNonNull(agents, "agents");
        this.leases = Objects.requireNonNull(leases, "leases");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.json = Objects.requireNonNull(json, "json");
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
        AgentRegistration agent = chooseAgent(snapshot).orElse(null);
        if (agent == null) return;

        Instant now = clock.instant();
        long epoch = leases.nextEpoch(workspaceId, runId);
        AgentLease lease = new AgentLease(ids.next("lease"), workspaceId, runId, agent.id(),
                agent.sessionId(), epoch, ids.next("fence"), AgentLease.Status.OFFERED,
                now, now.plus(offerTtl), null, null, 0, 0);
        LeaseFence fence = fence(lease);
        byte[] spec = executionSpecDocument(lease, snapshot, run);
        byte[] audience = audienceDocument(lease, snapshot);
        String leasePath = "/internal/agent/v1/leases/"
                + URLEncoder.encode(lease.id(), StandardCharsets.UTF_8);
        AgentFrames.LeaseOffer offer = new AgentFrames.LeaseOffer(fence, lease.expiresAt(),
                publicBaseUrl + leasePath + "/execution-spec", sha256(spec),
                publicBaseUrl + leasePath + "/audience", sha256(audience), new byte[0]);

        RunEventRecord offered = transactions.required(() -> {
            AgentLease current = leases.findCurrent(workspaceId, runId).orElse(null);
            if (current != null && current.status().active()) return null;
            RunDefinition latest = runs.findById(workspaceId, runId).orElse(null);
            if (latest == null || (latest.status() != RunStatus.PENDING
                    && latest.status() != RunStatus.RECOVERING)) return null;
            leases.create(lease);
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
        if (!gateway.send(agent.id(), offer)) {
            lose(lease, "AGENT_STREAM_UNAVAILABLE", true);
        }
    }

    public Optional<AgentFrames.ServicePayload> accept(AgentRegistration agent,
                                                       AgentFrames.AgentPayload payload) {
        if (payload instanceof AgentFrames.LeaseAck acknowledgement) {
            acknowledge(agent, acknowledgement.fence());
        } else if (payload instanceof AgentFrames.EventBatch batch) {
            return Optional.of(acceptEvents(agent, batch));
        } else if (payload instanceof AgentFrames.RunCompleted completed) {
            complete(agent, completed);
        } else if (payload instanceof AgentFrames.CommandAck commandAck) {
            validate(agent, commandAck.fence());
        }
        return Optional.empty();
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
        AgentFrames.RunCommand frame = new AgentFrames.RunCommand(command.commandId(), fence(lease),
                document.type(), document.payload(), clock.instant());
        return gateway.send(lease.agentId(), frame)
                ? CommandResult.accepted(command.commandId(), "REMOTE_COMMAND_DELIVERED")
                : CommandResult.rejected(command.commandId(), "AGENT_STREAM_UNAVAILABLE",
                "Agent control stream is unavailable");
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
        runs.list(DEFAULT_WORKSPACE).stream()
                .filter(run -> run.status() == RunStatus.PENDING || run.status() == RunStatus.RECOVERING)
                .forEach(run -> dispatch(DEFAULT_WORKSPACE, run.id()));
    }

    public void expireAndRecover() {
        for (AgentLease lease : transactions.required(() -> leases.expireActive(clock.instant()))) {
            recoverLease(lease, "LEASE_EXPIRED");
        }
        recoverPending();
    }

    public void disconnected(AgentRegistration agent) {
        List<AgentLease> active = leases.activeForAgent(agent.id(), agent.sessionId());
        for (AgentLease lease : active) lose(lease, "AGENT_DISCONNECTED", false);
    }

    private Optional<AgentRegistration> chooseAgent(RunSnapshot snapshot) {
        return agents.list().stream()
                .filter(agent -> agent.status() == AgentRegistration.Status.ONLINE)
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
        List<RemoteRunDocuments.Recipient> recipients = audiences.recipients(
                        lease.workspaceId(), snapshot.audienceSnapshotId()).stream()
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

    private static LeaseFence fence(AgentLease lease) {
        return new LeaseFence(lease.id(), lease.runId(), lease.epoch(), lease.fencingToken());
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
