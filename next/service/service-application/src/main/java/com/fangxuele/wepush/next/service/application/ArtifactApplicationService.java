package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.ArtifactDefinition;
import com.fangxuele.wepush.next.service.domain.ArtifactRepository;
import com.fangxuele.wepush.next.service.domain.RunDefinition;
import com.fangxuele.wepush.next.service.domain.RunEventRecord;
import com.fangxuele.wepush.next.service.domain.RunItemResultRecord;
import com.fangxuele.wepush.next.service.domain.RunRepository;
import com.fangxuele.wepush.next.service.domain.RunResultRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ArtifactApplicationService {
    public static final String RUN_RESULTS_CSV = "RUN_RESULTS_CSV";
    private static final String CSV_CONTENT_TYPE = "text/csv; charset=utf-8";

    private final RunRepository runs;
    private final RunResultRepository results;
    private final ArtifactRepository artifacts;
    private final WorkspaceResourceGovernor resources;
    private final ArtifactStore store;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final JsonCodec json;
    private final RunEventPublisher events;
    private final Clock clock;
    private final Duration exportRetention;

    public ArtifactApplicationService(RunRepository runs, RunResultRepository results,
                                      ArtifactRepository artifacts, WorkspaceResourceGovernor resources,
                                      ArtifactStore store,
                                      ResourceIdGenerator ids, TransactionRunner transactions,
                                      JsonCodec json, RunEventPublisher events, Clock clock,
                                      Duration exportRetention) {
        this.runs = runs;
        this.results = results;
        this.artifacts = artifacts;
        this.resources = resources;
        this.store = store;
        this.ids = ids;
        this.transactions = transactions;
        this.json = json;
        this.events = events;
        this.clock = clock;
        if (exportRetention == null || exportRetention.isNegative() || exportRetention.isZero()) {
            throw new IllegalArgumentException("export retention must be positive");
        }
        this.exportRetention = exportRetention;
    }

    public CreationResult createResultExport(WorkspaceId workspaceId, String runId) {
        RunDefinition run = requireRun(workspaceId, runId);
        if (!run.status().terminal()) {
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "RUN_NOT_TERMINAL",
                    "Result export can only be created after the Run reaches a terminal state");
        }
        Instant now = clock.instant();
        ArtifactDefinition existing = artifacts.findReadyByRunAndType(
                workspaceId, runId, RUN_RESULTS_CSV).orElse(null);
        if (existing != null && existing.expiresAt().isAfter(now)) {
            return new CreationResult(existing, true);
        }

        String artifactId = ids.next("artifact");
        ArtifactStore.ObjectPlan plan = store.plan(workspaceId, artifactId, RUN_RESULTS_CSV, now);
        var policy = resources.policy(workspaceId);
        Duration retention = policy.version() == 0 ? exportRetention : policy.artifactRetention();
        ArtifactDefinition uploading = new ArtifactDefinition(artifactId, workspaceId, runId,
                RUN_RESULTS_CSV, plan.backend(), plan.location(),
                "wepush-results-" + shortId(runId) + ".csv", CSV_CONTENT_TYPE,
                0, "", ArtifactDefinition.State.UPLOADING, now.plus(retention),
                false, false, now, null, null, "", 0);
        transactions.required(() -> {
            artifacts.create(uploading);
            return null;
        });

        try {
            ArtifactStore.StoredObject stored = store.write(plan,
                    output -> writeResultCsv(workspaceId, runId, output));
            RunEventRecord event = transactions.required(() -> {
                Instant readyAt = clock.instant();
                resources.requireArtifactCapacity(workspaceId, stored.size());
                artifacts.markReady(workspaceId, artifactId, stored.size(), stored.sha256(), readyAt);
                RunEventRecord recorded = new RunEventRecord(runId, workspaceId,
                        runs.nextEventSequence(workspaceId, runId), "ARTIFACT_READY", readyAt,
                        json.canonicalize(Map.of("artifactId", artifactId, "type", RUN_RESULTS_CSV,
                                "size", stored.size(), "sha256", stored.sha256())),
                        RunEventRecord.Severity.INFO);
                runs.appendEvent(recorded);
                return recorded;
            });
            events.publish(event);
            ArtifactDefinition ready = artifacts.findById(workspaceId, artifactId)
                    .orElseThrow(() -> new IllegalStateException("ready artifact metadata is missing"));
            return new CreationResult(ready, false);
        } catch (IOException | RuntimeException exception) {
            try {
                store.delete(plan.location());
            } catch (IOException deleteFailure) {
                exception.addSuppressed(deleteFailure);
            }
            transactions.required(() -> {
                artifacts.markFailed(workspaceId, artifactId,
                        "EXPORT_" + exception.getClass().getSimpleName());
                return null;
            });
            throw new IllegalStateException("result artifact export failed", exception);
        }
    }

    public List<ArtifactDefinition> listForRun(WorkspaceId workspaceId, String runId) {
        requireRun(workspaceId, runId);
        return artifacts.listForRun(workspaceId, runId);
    }

    public ArtifactDefinition get(WorkspaceId workspaceId, String artifactId) {
        return artifacts.findById(workspaceId, artifactId).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "ARTIFACT_NOT_FOUND",
                        "Artifact was not found: " + artifactId));
    }

    public Download open(WorkspaceId workspaceId, String artifactId, long offset, long length) {
        ArtifactDefinition artifact = get(workspaceId, artifactId);
        if (artifact.state() != ArtifactDefinition.State.READY) {
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "ARTIFACT_NOT_READY",
                    "Artifact content is not available in state " + artifact.state());
        }
        if (offset < 0 || offset > artifact.size() || length < 0 || length > artifact.size() - offset) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "INVALID_RANGE",
                    "Requested Artifact byte range is invalid");
        }
        try {
            return new Download(artifact, offset, length, store.open(artifact.location(), offset, length));
        } catch (IOException exception) {
            throw new IllegalStateException("artifact content cannot be opened", exception);
        }
    }

    public CleanupResult cleanupExpired(int limit) {
        Instant now = clock.instant();
        List<ArtifactDefinition> claimed = transactions.required(() -> artifacts.claimExpired(now, limit));
        int deleted = 0;
        int failed = 0;
        for (ArtifactDefinition artifact : claimed) {
            try {
                artifacts.findMultipart(artifact.workspaceId(), artifact.id()).ifPresent(upload -> {
                    try {
                        store.abortMultipartUpload(new ArtifactStore.ObjectPlan(
                                artifact.backend(), artifact.location()), upload.uploadId());
                    } catch (IOException problem) {
                        throw new MultipartCleanupException(problem);
                    }
                });
                store.delete(artifact.location());
                transactions.required(() -> {
                    artifacts.deleteMultipart(artifact.workspaceId(), artifact.id());
                    artifacts.markDeleted(artifact.workspaceId(), artifact.id(), clock.instant());
                    return null;
                });
                deleted++;
            } catch (IOException | RuntimeException exception) {
                failed++;
                transactions.required(() -> {
                    artifacts.markFailed(artifact.workspaceId(), artifact.id(),
                            "DELETE_" + exception.getClass().getSimpleName());
                    return null;
                });
            }
        }
        return new CleanupResult(claimed.size(), deleted, failed);
    }

    private RunDefinition requireRun(WorkspaceId workspaceId, String runId) {
        return runs.findById(workspaceId, runId).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "RUN_NOT_FOUND",
                        "Run was not found: " + runId));
    }

    private void writeResultCsv(WorkspaceId workspaceId, String runId, java.io.OutputStream output)
            throws IOException {
        Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write("item_id,state,attempts,provider_code,external_request_id,completed_at\n");
        Instant completedAfter = null;
        String itemAfter = null;
        while (true) {
            List<RunItemResultRecord> page = results.page(workspaceId, runId,
                    completedAfter, itemAfter, 500);
            for (RunItemResultRecord result : page) {
                csv(writer, result.itemId()); writer.write(',');
                csv(writer, result.state().name()); writer.write(',');
                writer.write(Integer.toString(result.attempts())); writer.write(',');
                csv(writer, result.providerCode()); writer.write(',');
                csv(writer, result.externalRequestId()); writer.write(',');
                csv(writer, result.completedAt().toString()); writer.write('\n');
            }
            if (page.size() < 500) break;
            RunItemResultRecord last = page.getLast();
            completedAfter = last.completedAt();
            itemAfter = last.itemId();
        }
        writer.flush();
    }

    private static void csv(Writer writer, String value) throws IOException {
        String safe = value == null ? "" : value;
        writer.write('"');
        writer.write(safe.replace("\"", "\"\""));
        writer.write('"');
    }

    private static String shortId(String value) {
        return value.length() <= 20 ? value : value.substring(0, 12);
    }

    public record CreationResult(ArtifactDefinition artifact, boolean replayed) {
    }

    public record Download(ArtifactDefinition artifact, long offset, long length,
                           InputStream content) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            content.close();
        }
    }

    public record CleanupResult(int claimed, int deleted, int failed) {
    }

    private static final class MultipartCleanupException extends RuntimeException {
        private MultipartCleanupException(IOException cause) { super(cause); }
    }
}
