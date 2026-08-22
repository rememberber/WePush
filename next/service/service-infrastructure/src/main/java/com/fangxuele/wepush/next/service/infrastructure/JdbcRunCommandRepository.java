package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.RunCommandRecord;
import com.fangxuele.wepush.next.service.domain.RunCommandRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Optional;

public final class JdbcRunCommandRepository implements RunCommandRepository {
    private final JdbcTemplate jdbc;

    public JdbcRunCommandRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<RunCommandRecord> findById(WorkspaceId workspaceId, String runId, String commandId) {
        return jdbc.query("""
                SELECT * FROM run_command WHERE workspace_id = ? AND run_id = ? AND id = ?
                """, JdbcRows.COMMAND, workspaceId.value(), runId, commandId).stream().findFirst();
    }

    @Override
    public boolean create(RunCommandRecord command) {
        return jdbc.update("""
                INSERT INTO run_command
                (id, workspace_id, run_id, type, payload_json, status, result_code, result_message,
                 created_at, acknowledged_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """, command.id(), command.workspaceId().value(), command.runId(), command.type(),
                command.payload().value(), command.status().name(), command.resultCode(),
                command.resultMessage(), command.createdAt().toString(), null) == 1;
    }

    @Override
    public void acknowledge(WorkspaceId workspaceId, String runId, String commandId,
                            RunCommandRecord.Status status, String resultCode, String resultMessage,
                            Instant acknowledgedAt) {
        jdbc.update("""
                UPDATE run_command
                SET status = ?, result_code = ?, result_message = ?, acknowledged_at = ?
                WHERE workspace_id = ? AND run_id = ? AND id = ? AND status = 'PENDING'
                """, status.name(), resultCode, resultMessage, acknowledgedAt.toString(),
                workspaceId.value(), runId, commandId);
    }
}
