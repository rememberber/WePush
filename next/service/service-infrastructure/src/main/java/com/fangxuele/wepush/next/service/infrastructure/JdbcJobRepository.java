package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.JobDefinition;
import com.fangxuele.wepush.next.service.domain.JobRepository;
import com.fangxuele.wepush.next.service.domain.ResourcePageQuery;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

public final class JdbcJobRepository implements JobRepository {
    private final JdbcTemplate jdbc;

    public JdbcJobRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void create(JobDefinition job) {
        jdbc.update("""
                INSERT INTO job_definition
                (id, workspace_id, name, account_id, message_id, audience_id, policies_json, enabled, archived,
                 created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, job.id(), job.workspaceId().value(), job.name(), job.accountId(), job.messageId(),
                job.audienceId(), job.policies().value(), job.enabled() ? 1 : 0, job.archived() ? 1 : 0,
                job.createdAt().toString(), job.updatedAt().toString(), job.version());
    }

    @Override
    public Optional<JobDefinition> findById(WorkspaceId workspaceId, String jobId) {
        return jdbc.query("SELECT * FROM job_definition WHERE workspace_id = ? AND id = ?", JdbcRows.JOB,
                workspaceId.value(), jobId).stream().findFirst();
    }

    @Override
    public List<JobDefinition> list(WorkspaceId workspaceId) {
        return jdbc.query("SELECT * FROM job_definition WHERE workspace_id = ? ORDER BY created_at DESC, id",
                JdbcRows.JOB, workspaceId.value());
    }

    @Override
    public List<JobDefinition> page(WorkspaceId workspaceId, ResourcePageQuery query) {
        JdbcPageQueries.Query page = JdbcPageQueries.build("SELECT * FROM job_definition",
                "workspace_id", workspaceId.value(), "name",
                "CASE WHEN archived = 1 THEN 'ARCHIVED' WHEN enabled = 1 THEN 'ACTIVE' ELSE 'DISABLED' END",
                "created_at", "id", query);
        return jdbc.query(page.sql(), JdbcRows.JOB, page.parameters());
    }

    @Override
    public boolean update(JobDefinition job, long expectedVersion) {
        return jdbc.update("""
                UPDATE job_definition
                SET name = ?, account_id = ?, message_id = ?, audience_id = ?, policies_json = ?,
                    enabled = ?, archived = ?, updated_at = ?, version = version + 1
                WHERE workspace_id = ? AND id = ? AND version = ?
                """, job.name(), job.accountId(), job.messageId(), job.audienceId(), job.policies().value(),
                job.enabled() ? 1 : 0, job.archived() ? 1 : 0, job.updatedAt().toString(),
                job.workspaceId().value(), job.id(), expectedVersion) == 1;
    }
}
