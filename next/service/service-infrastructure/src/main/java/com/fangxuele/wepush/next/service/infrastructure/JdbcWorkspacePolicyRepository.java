package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspacePolicy;
import com.fangxuele.wepush.next.service.domain.WorkspacePolicyRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public final class JdbcWorkspacePolicyRepository implements WorkspacePolicyRepository {
    private final JdbcTemplate jdbc;

    public JdbcWorkspacePolicyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<WorkspacePolicy> find(WorkspaceId workspaceId) {
        return jdbc.query("SELECT * FROM workspace_policy WHERE workspace_id = ?", this::row,
                workspaceId.value()).stream().findFirst();
    }

    @Override
    public void createDefault(WorkspaceId workspaceId, Instant now) {
        jdbc.update("""
                INSERT INTO workspace_policy
                (workspace_id, max_agents, max_concurrent_runs, max_total_concurrency,
                 artifact_quota_bytes, artifact_retention_seconds, updated_at, version)
                VALUES (?, 0, 0, 0, 0, ?, ?, 0)
                ON CONFLICT (workspace_id) DO NOTHING
                """, workspaceId.value(), WorkspacePolicy.DEFAULT_RETENTION_SECONDS, now.toString());
    }

    @Override
    public void save(WorkspacePolicy value, long expectedVersion) {
        int changed = jdbc.update("""
                UPDATE workspace_policy
                SET max_agents = ?, max_concurrent_runs = ?, max_total_concurrency = ?,
                    artifact_quota_bytes = ?, artifact_retention_seconds = ?, updated_at = ?,
                    version = version + 1
                WHERE workspace_id = ? AND version = ?
                """, value.maxAgents(), value.maxConcurrentRuns(), value.maxTotalConcurrency(),
                value.artifactQuotaBytes(), value.artifactRetentionSeconds(), value.updatedAt().toString(),
                value.workspaceId().value(), expectedVersion);
        if (changed != 1) throw new IllegalStateException("Workspace Policy was concurrently modified");
    }

    @Override
    public void lock(WorkspaceId workspaceId) {
        int changed = jdbc.update("""
                UPDATE workspace_policy SET updated_at = updated_at WHERE workspace_id = ?
                """, workspaceId.value());
        if (changed != 1) throw new IllegalStateException("Workspace Policy is missing");
    }

    @Override
    public Usage usage(WorkspaceId workspaceId) {
        Long agents = jdbc.queryForObject("""
                SELECT COUNT(*) FROM agent_workspace_binding WHERE workspace_id = ?
                """, Long.class, workspaceId.value());
        long[] runs = jdbc.queryForObject("""
                SELECT COUNT(*), COALESCE(SUM(r.target_concurrency), 0)
                FROM workspace_run_reservation r
                JOIN run_instance i ON i.id = r.run_id AND i.workspace_id = r.workspace_id
                WHERE r.workspace_id = ? AND i.status IN ('PENDING', 'RUNNING', 'PAUSING', 'PAUSED', 'CANCELING')
                """, (rs, ignored) -> new long[]{rs.getLong(1), rs.getLong(2)}, workspaceId.value());
        Long artifacts = jdbc.queryForObject("""
                SELECT COALESCE(SUM(size), 0) FROM artifact_record
                WHERE workspace_id = ? AND state IN ('UPLOADING', 'READY', 'DELETING')
                """, Long.class, workspaceId.value());
        return new Usage(value(agents), runs == null ? 0 : runs[0], runs == null ? 0 : runs[1], value(artifacts));
    }

    @Override
    public boolean agentAlreadyBound(WorkspaceId workspaceId, String agentId) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM agent_workspace_binding WHERE workspace_id = ? AND agent_id = ?
                """, Long.class, workspaceId.value(), agentId);
        return value(count) > 0;
    }

    @Override
    public void reserveRun(WorkspaceId workspaceId, String runId, int targetConcurrency, Instant createdAt) {
        jdbc.update("""
                INSERT INTO workspace_run_reservation
                (run_id, workspace_id, target_concurrency, created_at) VALUES (?, ?, ?, ?)
                """, runId, workspaceId.value(), targetConcurrency, createdAt.toString());
    }

    private WorkspacePolicy row(ResultSet rs, int ignored) throws SQLException {
        return new WorkspacePolicy(new WorkspaceId(rs.getString("workspace_id")),
                rs.getInt("max_agents"), rs.getInt("max_concurrent_runs"),
                rs.getInt("max_total_concurrency"), rs.getLong("artifact_quota_bytes"),
                rs.getLong("artifact_retention_seconds"), Instant.parse(rs.getString("updated_at")),
                rs.getLong("version"));
    }

    private static long value(Long value) {
        return value == null ? 0 : value;
    }
}
