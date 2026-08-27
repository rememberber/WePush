package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.IdempotencyRecord;
import com.fangxuele.wepush.next.service.domain.RunDefinition;
import com.fangxuele.wepush.next.service.domain.RunEventRecord;
import com.fangxuele.wepush.next.service.domain.RunRepository;
import com.fangxuele.wepush.next.service.domain.RunSnapshot;
import com.fangxuele.wepush.next.service.domain.RunStatus;
import com.fangxuele.wepush.next.service.domain.RunOverview;
import com.fangxuele.wepush.next.service.domain.ResourcePageQuery;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class JdbcRunRepository implements RunRepository {
    private final JdbcTemplate jdbc;

    public JdbcRunRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void create(RunDefinition run, RunSnapshot snapshot, RunEventRecord createdEvent,
                       IdempotencyRecord idempotencyRecord) {
        jdbc.update("""
                INSERT INTO run_instance
                (id, workspace_id, job_id, status, state_reason, dry_run, source_run_id, retry_states,
                 total, succeeded, failed, unknown_count, unsent, skipped, retried,
                 created_at, started_at, ended_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, run.id(), run.workspaceId().value(), run.jobId(), run.status().name(), run.stateReason(),
                run.dryRun() ? 1 : 0, run.sourceRunId(), run.retryStates(),
                run.total(), run.succeeded(), run.failed(), run.unknown(), run.unsent(),
                run.skipped(), run.retried(), text(run.createdAt()), text(run.startedAt()), text(run.endedAt()),
                text(run.updatedAt()), run.version());
        jdbc.update("""
                INSERT INTO run_snapshot
                (id, run_id, workspace_id, provider_id, provider_version, account_configuration_json,
                 message_content_json, policies_json, audience_snapshot_id, content_hash)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, snapshot.id(), snapshot.runId(), snapshot.workspaceId().value(),
                snapshot.provider().providerId(), snapshot.provider().implementationVersion(),
                snapshot.accountConfiguration().value(), snapshot.messageContent().value(),
                snapshot.policies().value(), snapshot.audienceSnapshotId(), snapshot.contentHash());
        appendEvent(createdEvent);
        int idempotencyChanged = jdbc.update("""
                INSERT INTO idempotency_record
                (workspace_id, scope, key_hash, request_hash, resource_id, response_status, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (workspace_id, scope, key_hash) DO UPDATE SET
                    request_hash = excluded.request_hash,
                    resource_id = excluded.resource_id,
                    response_status = excluded.response_status,
                    created_at = excluded.created_at,
                    expires_at = excluded.expires_at
                WHERE idempotency_record.expires_at <= excluded.created_at
                """, idempotencyRecord.workspaceId().value(), idempotencyRecord.scope(), idempotencyRecord.keyHash(),
                idempotencyRecord.requestHash(), idempotencyRecord.resourceId(), idempotencyRecord.responseStatus(),
                text(idempotencyRecord.createdAt()), text(idempotencyRecord.expiresAt()));
        if (idempotencyChanged != 1) {
            throw new IllegalStateException("idempotency key was concurrently claimed");
        }
    }

    @Override
    public Optional<RunDefinition> findById(WorkspaceId workspaceId, String runId) {
        return jdbc.query("SELECT * FROM run_instance WHERE workspace_id = ? AND id = ?", JdbcRows.RUN,
                workspaceId.value(), runId).stream().findFirst();
    }

    @Override
    public Optional<RunSnapshot> findSnapshot(WorkspaceId workspaceId, String runId) {
        return jdbc.query("SELECT * FROM run_snapshot WHERE workspace_id = ? AND run_id = ?", JdbcRows.SNAPSHOT,
                workspaceId.value(), runId).stream().findFirst();
    }

    @Override
    public List<RunDefinition> list(WorkspaceId workspaceId) {
        return jdbc.query("SELECT * FROM run_instance WHERE workspace_id = ? ORDER BY created_at DESC, id",
                JdbcRows.RUN, workspaceId.value());
    }

    @Override
    public List<RunDefinition> page(WorkspaceId workspaceId, ResourcePageQuery query) {
        JdbcPageQueries.Query page = JdbcPageQueries.build("""
                SELECT r.* FROM run_instance r JOIN job_definition j ON j.id = r.job_id
                """, "r.workspace_id", workspaceId.value(), "j.name", "r.status",
                "r.created_at", "r.id", query);
        return jdbc.query(page.sql(), JdbcRows.RUN, page.parameters());
    }

    @Override
    public List<RunDefinition> active(WorkspaceId workspaceId, int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("active run limit must be between 1 and 100");
        }
        return jdbc.query("""
                SELECT * FROM run_instance
                WHERE workspace_id = ? AND status IN ('PENDING','RECOVERING','RUNNING','PAUSED')
                ORDER BY created_at DESC, id DESC LIMIT ?
                """, JdbcRows.RUN, workspaceId.value(), limit);
    }

    @Override
    public RunOverview overview(WorkspaceId workspaceId, Instant from) {
        long[] summary = jdbc.queryForObject("""
                SELECT COUNT(*),
                       COALESCE(SUM(CASE WHEN status IN ('PENDING','RECOVERING','RUNNING','PAUSED') THEN 1 ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN status = 'SUCCEEDED' THEN 1 ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN status IN ('PARTIAL','FAILED') THEN 1 ELSE 0 END), 0)
                FROM run_instance WHERE workspace_id = ?
                """, (rs, ignored) -> new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)},
                workspaceId.value());
        List<RunOverview.TrendPoint> trend = jdbc.query("""
                SELECT SUBSTRING(created_at, 1, 10) AS day, COUNT(*) AS total,
                       COALESCE(SUM(CASE WHEN status = 'SUCCEEDED' THEN 1 ELSE 0 END), 0) AS succeeded,
                       COALESCE(SUM(CASE WHEN status IN ('PARTIAL','FAILED') THEN 1 ELSE 0 END), 0) AS problem
                FROM run_instance WHERE workspace_id = ? AND created_at >= ?
                GROUP BY SUBSTRING(created_at, 1, 10) ORDER BY day
                """, (rs, ignored) -> new RunOverview.TrendPoint(java.time.LocalDate.parse(rs.getString("day")),
                        rs.getLong("total"), rs.getLong("succeeded"), rs.getLong("problem")),
                workspaceId.value(), from.toString());
        return new RunOverview(summary[1], summary[0], summary[2], summary[3], trend);
    }

    @Override
    public void createRetry(RunDefinition run, RunSnapshot snapshot, RunEventRecord createdEvent,
                            IdempotencyRecord idempotencyRecord, String sourceRunId,
                            Set<String> retryStates) {
        create(run, snapshot, createdEvent, idempotencyRecord);
        String markers = String.join(",", java.util.Collections.nCopies(retryStates.size(), "?"));
        String sql = "INSERT INTO run_retry_item(run_id, workspace_id, item_id) "
                + "SELECT ?, workspace_id, item_id FROM run_item_result "
                + "WHERE workspace_id = ? AND run_id = ? AND state IN (" + markers + ")";
        List<Object> parameters = new ArrayList<>();
        parameters.add(run.id());
        parameters.add(run.workspaceId().value());
        parameters.add(sourceRunId);
        retryStates.stream().sorted().forEach(parameters::add);
        jdbc.update(sql, parameters.toArray());
    }

    @Override
    public Optional<IdempotencyRecord> findIdempotency(WorkspaceId workspaceId, String scope, String keyHash) {
        return jdbc.query("""
                SELECT * FROM idempotency_record
                WHERE workspace_id = ? AND scope = ? AND key_hash = ?
                """, JdbcRows.IDEMPOTENCY, workspaceId.value(), scope, keyHash).stream().findFirst();
    }

    @Override
    public boolean transition(WorkspaceId workspaceId, String runId, Set<RunStatus> expected,
                              RunStatus target, String reason, Instant changedAt) {
        if (expected.isEmpty()) {
            throw new IllegalArgumentException("expected run statuses must not be empty");
        }
        String markers = String.join(",", java.util.Collections.nCopies(expected.size(), "?"));
        String sql = "UPDATE run_instance SET status = ?, state_reason = ?, updated_at = ?, "
                + "started_at = CASE WHEN ? = 'RUNNING' AND started_at IS NULL THEN ? ELSE started_at END, "
                + "version = version + 1 WHERE workspace_id = ? AND id = ? AND status IN (" + markers + ")";
        List<Object> parameters = new ArrayList<>();
        parameters.add(target.name());
        parameters.add(reason == null ? "" : reason);
        parameters.add(text(changedAt));
        parameters.add(target.name());
        parameters.add(text(changedAt));
        parameters.add(workspaceId.value());
        parameters.add(runId);
        expected.stream().map(Enum::name).sorted().forEach(parameters::add);
        return jdbc.update(sql, parameters.toArray()) == 1;
    }

    @Override
    public void complete(WorkspaceId workspaceId, String runId, RunStatus target, String reason,
                         long total, long succeeded, long failed, long unknown, long unsent,
                         long skipped, long retried, Instant endedAt) {
        if (!target.terminal()) {
            throw new IllegalArgumentException("completion target must be terminal");
        }
        int changed = jdbc.update("""
                UPDATE run_instance
                SET status = ?, state_reason = ?, total = ?, succeeded = ?, failed = ?, unknown_count = ?,
                    unsent = ?, skipped = ?, retried = ?, ended_at = ?, updated_at = ?, version = version + 1
                WHERE workspace_id = ? AND id = ? AND status NOT IN ('CANCELLED', 'SUCCEEDED', 'PARTIAL', 'FAILED')
                """, target.name(), reason == null ? "" : reason, total, succeeded, failed, unknown, unsent,
                skipped, retried, text(endedAt), text(endedAt), workspaceId.value(), runId);
        if (changed != 1) {
            throw new IllegalStateException("run cannot be completed from its current state: " + runId);
        }
    }

    @Override
    public void appendEvent(RunEventRecord event) {
        jdbc.update("""
                INSERT INTO run_event
                (run_id, workspace_id, sequence, type, occurred_at, payload_json, severity)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, event.runId(), event.workspaceId().value(), event.sequence(), event.type(),
                text(event.occurredAt()), event.payload().value(), event.severity().name());
    }

    @Override
    public long nextEventSequence(WorkspaceId workspaceId, String runId) {
        Long sequence = jdbc.queryForObject("""
                SELECT COALESCE(MAX(sequence), 0) + 1 FROM run_event
                WHERE workspace_id = ? AND run_id = ?
                """, Long.class, workspaceId.value(), runId);
        return sequence == null ? 1 : sequence;
    }

    @Override
    public List<RunEventRecord> eventsAfter(WorkspaceId workspaceId, String runId,
                                            long sequenceExclusive, int limit) {
        if (limit < 1 || limit > 1_000) {
            throw new IllegalArgumentException("event limit must be between 1 and 1000");
        }
        return jdbc.query("""
                SELECT * FROM run_event
                WHERE workspace_id = ? AND run_id = ? AND sequence > ?
                ORDER BY sequence LIMIT ?
                """, JdbcRows.EVENT, workspaceId.value(), runId, sequenceExclusive, limit);
    }

    private static String text(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
