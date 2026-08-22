package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.AgentLease;
import com.fangxuele.wepush.next.service.domain.AgentLeaseRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class JdbcAgentLeaseRepository implements AgentLeaseRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<AgentLease> rowMapper = this::row;

    public JdbcAgentLeaseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<AgentLease> findById(String leaseId) {
        return jdbc.query("SELECT * FROM agent_lease WHERE id = ?", rowMapper, leaseId)
                .stream().findFirst();
    }

    @Override
    public Optional<AgentLease> findCurrent(WorkspaceId workspaceId, String runId) {
        return jdbc.query("""
                SELECT * FROM agent_lease
                WHERE workspace_id = ? AND run_id = ?
                ORDER BY epoch DESC LIMIT 1
                """, rowMapper, workspaceId.value(), runId).stream().findFirst();
    }

    @Override
    public List<AgentLease> activeForAgent(String agentId, String agentSessionId) {
        return jdbc.query("""
                SELECT * FROM agent_lease
                WHERE agent_id = ? AND agent_session_id = ?
                  AND status IN ('OFFERED', 'ACKNOWLEDGED', 'RUNNING')
                ORDER BY assigned_at, id
                """, rowMapper, agentId, agentSessionId);
    }

    @Override
    public int offeredCount(String agentId, String agentSessionId) {
        Integer value = jdbc.queryForObject("""
                SELECT COUNT(*) FROM agent_lease
                WHERE agent_id = ? AND agent_session_id = ? AND status = 'OFFERED'
                """, Integer.class, agentId, agentSessionId);
        return value == null ? 0 : value;
    }

    @Override
    public long nextEpoch(WorkspaceId workspaceId, String runId) {
        Long value = jdbc.queryForObject("""
                SELECT COALESCE(MAX(epoch), 0) + 1 FROM agent_lease
                WHERE workspace_id = ? AND run_id = ?
                """, Long.class, workspaceId.value(), runId);
        return value == null ? 1 : value;
    }

    @Override
    public void create(AgentLease value) {
        jdbc.update("""
                INSERT INTO agent_lease
                (id, workspace_id, run_id, agent_id, agent_session_id, epoch, fencing_token,
                 status, assigned_at, expires_at, acknowledged_at, completed_at,
                 last_event_sequence, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, value.id(), value.workspaceId().value(), value.runId(), value.agentId(),
                value.agentSessionId(), value.epoch(), value.fencingToken(), value.status().name(),
                text(value.assignedAt()), text(value.expiresAt()), text(value.acknowledgedAt()),
                text(value.completedAt()), value.lastEventSequence(), value.version());
    }

    @Override
    public boolean acknowledge(String leaseId, String agentId, String agentSessionId,
                               String fencingToken, Instant acknowledgedAt) {
        return jdbc.update("""
                UPDATE agent_lease
                SET status = 'ACKNOWLEDGED', acknowledged_at = ?, version = version + 1
                WHERE id = ? AND agent_id = ? AND agent_session_id = ? AND fencing_token = ?
                  AND status = 'OFFERED' AND expires_at > ?
                """, text(acknowledgedAt), leaseId, agentId, agentSessionId, fencingToken,
                text(acknowledgedAt)) == 1;
    }

    @Override
    public boolean markRunning(String leaseId, String fencingToken) {
        return jdbc.update("""
                UPDATE agent_lease SET status = 'RUNNING', version = version + 1
                WHERE id = ? AND fencing_token = ? AND status = 'ACKNOWLEDGED'
                """, leaseId, fencingToken) == 1;
    }

    @Override
    public boolean advanceEvents(String leaseId, String fencingToken, long expectedPrevious,
                                 long lastEventSequence) {
        return jdbc.update("""
                UPDATE agent_lease
                SET last_event_sequence = ?, version = version + 1
                WHERE id = ? AND fencing_token = ? AND status IN ('ACKNOWLEDGED', 'RUNNING')
                  AND last_event_sequence = ?
                """, lastEventSequence, leaseId, fencingToken, expectedPrevious) == 1;
    }

    @Override
    public boolean complete(String leaseId, String fencingToken, Instant completedAt) {
        return jdbc.update("""
                UPDATE agent_lease
                SET status = 'COMPLETED', completed_at = ?, version = version + 1
                WHERE id = ? AND fencing_token = ? AND status IN ('ACKNOWLEDGED', 'RUNNING')
                """, text(completedAt), leaseId, fencingToken) == 1;
    }

    @Override
    public boolean markLost(String leaseId, String fencingToken, Instant changedAt) {
        return jdbc.update("""
                UPDATE agent_lease
                SET status = 'LOST', completed_at = ?, version = version + 1
                WHERE id = ? AND fencing_token = ? AND status IN ('OFFERED', 'ACKNOWLEDGED', 'RUNNING')
                """, text(changedAt), leaseId, fencingToken) == 1;
    }

    @Override
    public List<AgentLease> expireActive(Instant now) {
        List<AgentLease> expired = jdbc.query("""
                SELECT * FROM agent_lease
                WHERE status IN ('OFFERED', 'ACKNOWLEDGED') AND expires_at <= ?
                ORDER BY expires_at, id
                """, rowMapper, text(now));
        if (!expired.isEmpty()) {
            jdbc.update("""
                    UPDATE agent_lease
                    SET status = 'EXPIRED', completed_at = ?, version = version + 1
                    WHERE status IN ('OFFERED', 'ACKNOWLEDGED') AND expires_at <= ?
                    """, text(now), text(now));
        }
        return expired;
    }

    private AgentLease row(ResultSet rs, int ignored) throws SQLException {
        return new AgentLease(rs.getString("id"), new WorkspaceId(rs.getString("workspace_id")),
                rs.getString("run_id"), rs.getString("agent_id"), rs.getString("agent_session_id"),
                rs.getLong("epoch"), rs.getString("fencing_token"),
                AgentLease.Status.valueOf(rs.getString("status")),
                Instant.parse(rs.getString("assigned_at")), Instant.parse(rs.getString("expires_at")),
                instant(rs.getString("acknowledged_at")), instant(rs.getString("completed_at")),
                rs.getLong("last_event_sequence"), rs.getLong("version"));
    }

    private static String text(Instant value) {
        return value == null ? null : value.toString();
    }

    private static Instant instant(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
