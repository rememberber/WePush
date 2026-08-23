package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.AgentOutboundMessage;
import com.fangxuele.wepush.next.service.domain.AgentOutboundMessageRepository;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public final class JdbcAgentOutboundMessageRepository implements AgentOutboundMessageRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<AgentOutboundMessage> rows = this::row;

    public JdbcAgentOutboundMessageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean create(AgentOutboundMessage value) {
        return jdbc.update("""
                INSERT INTO agent_message_outbox
                (id, workspace_id, run_id, agent_id, lease_id, message_type, command_type,
                 payload_json, created_at, next_attempt_at, delivered_at, acknowledged_at,
                 attempts, last_error)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """, value.id(), value.workspaceId().value(), value.runId(), value.agentId(),
                value.leaseId(), value.type().name(), value.commandType(), value.payload().value(),
                text(value.createdAt()), text(value.nextAttemptAt()), text(value.deliveredAt()),
                text(value.acknowledgedAt()), value.attempts(), value.lastError()) == 1;
    }

    @Override
    public List<AgentOutboundMessage> pending(Instant now, int limit) {
        return jdbc.query("""
                SELECT * FROM agent_message_outbox
                WHERE acknowledged_at IS NULL AND next_attempt_at <= ?
                ORDER BY next_attempt_at, created_at, id LIMIT ?
                """, rows, text(now), limit);
    }

    @Override
    public List<AgentOutboundMessage> pendingForAgent(String agentId, Instant now, int limit) {
        return jdbc.query("""
                SELECT * FROM agent_message_outbox
                WHERE agent_id = ? AND acknowledged_at IS NULL AND next_attempt_at <= ?
                ORDER BY next_attempt_at, created_at, id LIMIT ?
                """, rows, agentId, text(now), limit);
    }

    @Override
    public void delivered(String id, Instant deliveredAt, Instant nextAttemptAt) {
        jdbc.update("""
                UPDATE agent_message_outbox
                SET delivered_at = ?, next_attempt_at = ?, attempts = attempts + 1, last_error = ''
                WHERE id = ? AND acknowledged_at IS NULL
                """, text(deliveredAt), text(nextAttemptAt), id);
    }

    @Override
    public void failed(String id, String error, Instant nextAttemptAt) {
        jdbc.update("""
                UPDATE agent_message_outbox
                SET next_attempt_at = ?, attempts = attempts + 1, last_error = ?
                WHERE id = ? AND acknowledged_at IS NULL
                """, text(nextAttemptAt), bounded(error), id);
    }

    @Override
    public void acknowledgeLease(String leaseId, Instant acknowledgedAt) {
        jdbc.update("""
                UPDATE agent_message_outbox SET acknowledged_at = ?, last_error = ''
                WHERE lease_id = ? AND message_type = 'LEASE_OFFER' AND acknowledged_at IS NULL
                """, text(acknowledgedAt), leaseId);
    }

    @Override
    public void acknowledgeCommand(String commandId, Instant acknowledgedAt) {
        jdbc.update("""
                UPDATE agent_message_outbox SET acknowledged_at = ?, last_error = ''
                WHERE id = ? AND message_type = 'RUN_COMMAND' AND acknowledged_at IS NULL
                """, text(acknowledgedAt), commandId);
    }

    @Override
    public void discardLease(String leaseId, String reason, Instant discardedAt) {
        jdbc.update("""
                UPDATE agent_message_outbox SET acknowledged_at = ?, last_error = ?
                WHERE lease_id = ? AND acknowledged_at IS NULL
                """, text(discardedAt), bounded(reason), leaseId);
    }

    private AgentOutboundMessage row(ResultSet rs, int ignored) throws SQLException {
        return new AgentOutboundMessage(rs.getString("id"),
                new WorkspaceId(rs.getString("workspace_id")), rs.getString("run_id"),
                rs.getString("agent_id"), rs.getString("lease_id"),
                AgentOutboundMessage.Type.valueOf(rs.getString("message_type")),
                rs.getString("command_type"), new JsonDocument(rs.getString("payload_json")),
                Instant.parse(rs.getString("created_at")), Instant.parse(rs.getString("next_attempt_at")),
                instant(rs.getString("delivered_at")), instant(rs.getString("acknowledged_at")),
                rs.getInt("attempts"), rs.getString("last_error"));
    }

    private static String text(Instant value) {
        return value == null ? null : value.toString();
    }

    private static Instant instant(String value) {
        return value == null ? null : Instant.parse(value);
    }

    private static String bounded(String value) {
        if (value == null) return "";
        return value.length() <= 1_000 ? value : value.substring(0, 1_000);
    }
}
