package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.core.api.ArtifactSink;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.CommandResult;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.ExecutionEngine;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.ExecutionPorts;
import com.fangxuele.wepush.next.core.api.ItemResult;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RecipientSource;
import com.fangxuele.wepush.next.core.api.RecipientValue;
import com.fangxuele.wepush.next.core.api.ResultSink;
import com.fangxuele.wepush.next.core.api.RunEvent;
import com.fangxuele.wepush.next.core.api.RunEventSink;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.core.api.RunCommand;
import com.fangxuele.wepush.next.core.api.RunHandle;
import com.fangxuele.wepush.next.core.api.RunState;
import com.fangxuele.wepush.next.core.api.RunSummary;
import com.fangxuele.wepush.next.core.engine.DefaultExecutionEngine;
import com.fangxuele.wepush.next.service.application.JsonCodec;
import com.fangxuele.wepush.next.service.application.ProviderRegistry;
import com.fangxuele.wepush.next.service.application.RunDispatcher;
import com.fangxuele.wepush.next.service.application.RunCommandGateway;
import com.fangxuele.wepush.next.service.application.RunEventPublisher;
import com.fangxuele.wepush.next.service.application.SecretStore;
import com.fangxuele.wepush.next.service.application.TransactionRunner;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StandaloneRunExecutor implements RunDispatcher, RunCommandGateway, AutoCloseable {
    private final RunRepository runs;
    private final AudienceRepository audiences;
    private final RunResultRepository results;
    private final SecretStore secrets;
    private final JsonCodec json;
    private final TransactionRunner transactions;
    private final RunEventPublisher eventPublisher;
    private final Clock clock;
    private final ExecutionEngine engine;
    private final ExecutorService dispatcher = Executors.newVirtualThreadPerTaskExecutor();
    private final Set<String> activeRuns = ConcurrentHashMap.newKeySet();
    private final Map<String, RunHandle> handles = new ConcurrentHashMap<>();

    public StandaloneRunExecutor(RunRepository runs, RunResultRepository results, AudienceRepository audiences,
                                 ProviderRegistry providers, SecretStore secrets, JsonCodec json,
                                 TransactionRunner transactions, RunEventPublisher eventPublisher,
                                 Clock clock) {
        this.runs = runs;
        this.results = results;
        this.audiences = audiences;
        this.secrets = secrets;
        this.json = json;
        this.transactions = transactions;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.engine = new DefaultExecutionEngine(providers.providers());
    }

    @Override
    public void dispatch(WorkspaceId workspaceId, String runId) {
        String key = workspaceId.value() + ":" + runId;
        if (activeRuns.add(key)) {
            dispatcher.execute(() -> start(workspaceId, runId, key));
        }
    }

    public void recoverPending() {
        // Standalone has one implicit workspace in the first milestone.
        WorkspaceId workspaceId = new WorkspaceId("ws_default");
        runs.list(workspaceId).stream()
                .filter(run -> run.status() == RunStatus.PENDING || run.status() == RunStatus.RECOVERING)
                .forEach(run -> dispatch(workspaceId, run.id()));
    }

    @Override
    public CommandResult submit(WorkspaceId workspaceId, String runId, RunCommand command) {
        RunHandle handle = handles.get(runKey(workspaceId, runId));
        if (handle == null) {
            return CommandResult.rejected(command.commandId(), "RUN_NOT_ACTIVE",
                    "Run has no active embedded execution handle");
        }
        return handle.submit(command);
    }

    @Override
    public void close() {
        dispatcher.shutdown();
        engine.close();
    }

    private void start(WorkspaceId workspaceId, String runId, String activeKey) {
        try {
            RunDefinition run = runs.findById(workspaceId, runId).orElse(null);
            if (run == null || (run.status() != RunStatus.PENDING && run.status() != RunStatus.RECOVERING)) {
                activeRuns.remove(activeKey);
                return;
            }
            RunSnapshot snapshot = runs.findSnapshot(workspaceId, runId)
                    .orElseThrow(() -> new IllegalStateException("run snapshot is missing"));
            List<AudienceRecipient> storedRecipients = audiences.recipients(
                    workspaceId, snapshot.audienceSnapshotId());
            List<RecipientRecord> recipients = storedRecipients.stream().map(this::recipient).toList();
            ExecutionPolicies policies = ExecutionPolicyReader.read(json.read(snapshot.policies(), Map.class));
            RunExecutionSpec spec = new RunExecutionSpec(runId, snapshot.provider(),
                    config(snapshot.accountConfiguration(), "account"),
                    config(snapshot.messageContent(), "message"), policies,
                    Map.of("workspaceId", workspaceId.value(), "runSnapshotId", snapshot.id()),
                    run.dryRun(), run.createdAt());
            ExecutionPorts ports = new ExecutionPorts(new ListRecipientSource(recipients),
                    ref -> secrets.resolve(workspaceId, ref), new PersistentResultSink(workspaceId, runId),
                    ArtifactSink.none(), new PersistentEventSink(workspaceId),
                    executionClock());

            RunHandle handle = engine.start(spec, ports);
            handles.put(activeKey, handle);
            handle.completion().whenComplete((summary, failure) -> {
                try {
                    if (failure != null) {
                        fail(workspaceId, runId, "ENGINE_COMPLETION_FAILED");
                    } else {
                        finalizeRun(workspaceId, summary);
                    }
                } finally {
                    handles.remove(activeKey, handle);
                    activeRuns.remove(activeKey);
                }
            });
        } catch (RuntimeException exception) {
            try {
                fail(workspaceId, runId, "ENGINE_START_FAILED");
            } finally {
                handles.remove(activeKey);
                activeRuns.remove(activeKey);
            }
        }
    }

    private void finalizeRun(WorkspaceId workspaceId, RunSummary summary) {
        RunEventRecord finalized = transactions.required(() -> {
            RunStatus status = status(summary.finalState());
            runs.complete(workspaceId, summary.runId(), status, status.name().toLowerCase(),
                    summary.total(), summary.succeeded(), summary.failed(), summary.unknown(),
                    summary.unsent(), summary.skipped(), summary.retried(), summary.endedAt());
            RunEventRecord event = event(workspaceId, summary.runId(), "RUN_FINALIZED", summary.endedAt(),
                    Map.of("state", status.name(), "total", summary.total(), "succeeded", summary.succeeded(),
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

    private void fail(WorkspaceId workspaceId, String runId, String reason) {
        RunDefinition run = runs.findById(workspaceId, runId).orElse(null);
        if (run == null || run.status().terminal()) {
            return;
        }
        RunEventRecord failed = transactions.required(() -> {
            Instant now = clock.instant();
            runs.complete(workspaceId, runId, RunStatus.FAILED, reason,
                    run.total(), 0, 0, 0, run.total(), 0, 0, now);
            RunEventRecord event = event(workspaceId, runId, "RUN_FAILED_TO_START", now,
                    Map.of("code", reason), RunEventRecord.Severity.ERROR);
            runs.appendEvent(event);
            return event;
        });
        eventPublisher.publish(failed);
    }

    private RecipientRecord recipient(AudienceRecipient stored) {
        Map<?, ?> values = json.read(stored.fields(), Map.class);
        Map<String, RecipientValue> fields = new LinkedHashMap<>();
        values.forEach((key, value) -> fields.put(String.valueOf(key), recipientValue(value)));
        return new RecipientRecord(stored.itemId(), stored.sequence(), fields);
    }

    private RecipientValue recipientValue(Object value) {
        if (value == null) {
            return RecipientValue.NullValue.INSTANCE;
        }
        if (value instanceof String text) {
            return new RecipientValue.TextValue(text);
        }
        if (value instanceof Number number) {
            return new RecipientValue.NumberValue(new BigDecimal(number.toString()));
        }
        if (value instanceof Boolean bool) {
            return new RecipientValue.BooleanValue(bool);
        }
        return new RecipientValue.TextValue(json.canonicalize(value).value());
    }

    private ConfigDocument config(JsonDocument document, String kind) {
        return new ConfigDocument("wepush/run-snapshot/" + kind, "1",
                ConfigDocument.JSON_MEDIA_TYPE, document.value().getBytes(StandardCharsets.UTF_8));
    }

    private ExecutionClock executionClock() {
        return new ExecutionClock() {
            @Override
            public Instant now() {
                return clock.instant();
            }

            @Override
            public void sleep(java.time.Duration duration) throws InterruptedException {
                Thread.sleep(duration);
            }
        };
    }

    private RunEventRecord event(WorkspaceId workspaceId, String runId, String type,
                                 Instant occurredAt, Object payload, RunEventRecord.Severity severity) {
        return new RunEventRecord(runId, workspaceId, runs.nextEventSequence(workspaceId, runId),
                type, occurredAt, json.canonicalize(payload), severity);
    }

    private static RunStatus status(RunState state) {
        return switch (state) {
            case CANCELLED -> RunStatus.CANCELLED;
            case SUCCEEDED -> RunStatus.SUCCEEDED;
            case PARTIAL -> RunStatus.PARTIAL;
            case FAILED -> RunStatus.FAILED;
            default -> throw new IllegalArgumentException("execution did not end in a terminal state: " + state);
        };
    }

    private static String runKey(WorkspaceId workspaceId, String runId) {
        return workspaceId.value() + ":" + runId;
    }

    private final class PersistentEventSink implements RunEventSink {
        private final WorkspaceId workspaceId;

        private PersistentEventSink(WorkspaceId workspaceId) {
            this.workspaceId = workspaceId;
        }

        @Override
        public void append(RunEvent source) {
            RunEventRecord persisted = transactions.required(() -> {
                if (source.type() == RunEvent.Type.RUN_STARTED) {
                    runs.transition(workspaceId, source.runId(), Set.of(RunStatus.PENDING, RunStatus.RECOVERING),
                            RunStatus.RUNNING, "embedded-engine", source.occurredAt());
                }
                RunEventRecord event = event(workspaceId, source.runId(), source.type().name(),
                        source.occurredAt(), source.data(),
                        source.type() == RunEvent.Type.RUN_FAILED
                                ? RunEventRecord.Severity.ERROR : RunEventRecord.Severity.INFO);
                runs.appendEvent(event);
                return event;
            });
            eventPublisher.publish(persisted);
        }

        @Override
        public void flush() {
        }
    }

    private final class PersistentResultSink implements ResultSink {
        private final WorkspaceId workspaceId;
        private final String runId;

        private PersistentResultSink(WorkspaceId workspaceId, String runId) {
            this.workspaceId = workspaceId;
            this.runId = runId;
        }

        @Override
        public void append(List<ItemResult> batch) {
            if (batch == null || batch.isEmpty()) {
                return;
            }
            List<RunItemResultRecord> records = batch.stream().map(source -> {
                if (!runId.equals(source.runId())) {
                    throw new IllegalArgumentException("result belongs to another run");
                }
                return new RunItemResultRecord(source.runId(), workspaceId, source.itemId(), source.attempts(),
                        source.state(), source.providerCode(), source.diagnostic(), source.externalRequestId(),
                        source.completedAt(), json.canonicalize(source.metadata()));
            }).toList();
            transactions.required(() -> {
                results.append(records);
                return null;
            });
        }

        @Override
        public void flush() {
        }
    }

    private static final class ListRecipientSource implements RecipientSource {
        private final List<RecipientRecord> recipients;
        private int offset;

        private ListRecipientSource(List<RecipientRecord> recipients) {
            this.recipients = List.copyOf(recipients);
        }

        @Override
        public long totalCount() {
            return recipients.size();
        }

        @Override
        public synchronized List<RecipientRecord> nextBatch(int maximumSize) {
            if (offset >= recipients.size()) {
                return List.of();
            }
            int end = Math.min(recipients.size(), offset + maximumSize);
            List<RecipientRecord> batch = new ArrayList<>(recipients.subList(offset, end));
            offset = end;
            return batch;
        }
    }

}
