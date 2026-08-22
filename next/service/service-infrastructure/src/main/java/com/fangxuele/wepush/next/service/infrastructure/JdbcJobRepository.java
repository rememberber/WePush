package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.JobDefinition;
import com.fangxuele.wepush.next.service.domain.JobRepository;
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
                (id, workspace_id, name, account_id, message_id, audience_id, policies_json, enabled,
                 created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, job.id(), job.workspaceId().value(), job.name(), job.accountId(), job.messageId(),
                job.audienceId(), job.policies().value(), job.enabled() ? 1 : 0,
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
}
