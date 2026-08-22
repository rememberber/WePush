package com.fangxuele.wepush.next.service.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.service.domain.AgentRegistration;
import com.fangxuele.wepush.next.service.domain.AgentRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class JdbcAgentRepository implements AgentRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final RowMapper<AgentRegistration> rowMapper = this::row;

    public JdbcAgentRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public Optional<AgentRegistration> findById(String agentId) {
        return jdbc.query("SELECT * FROM agent_registration WHERE id = ?", rowMapper, agentId)
                .stream().findFirst();
    }

    @Override
    public List<AgentRegistration> list() {
        return jdbc.query("SELECT * FROM agent_registration ORDER BY last_seen_at DESC, id", rowMapper);
    }

    @Override
    public void connect(AgentRegistration value) {
        jdbc.update("""
                INSERT INTO agent_registration
                (id, status, agent_version, protocol_version, os_name, architecture, java_version,
                 maximum_runs, active_runs, available_runs, providers_json, session_id,
                 last_agent_sequence, last_service_sequence, connected_at, last_seen_at,
                 disconnected_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                  status = excluded.status, agent_version = excluded.agent_version,
                  protocol_version = excluded.protocol_version, os_name = excluded.os_name,
                  architecture = excluded.architecture, java_version = excluded.java_version,
                  maximum_runs = excluded.maximum_runs, active_runs = excluded.active_runs,
                  available_runs = excluded.available_runs, providers_json = excluded.providers_json,
                  session_id = excluded.session_id, last_agent_sequence = excluded.last_agent_sequence,
                  last_service_sequence = excluded.last_service_sequence,
                  connected_at = excluded.connected_at, last_seen_at = excluded.last_seen_at,
                  disconnected_at = NULL, version = excluded.version
                """, value.id(), value.status().name(), value.agentVersion(), value.protocolVersion(),
                value.operatingSystem(), value.architecture(), value.javaVersion(), value.maximumRuns(),
                value.activeRuns(), value.availableRuns(), json(value.providers()), value.sessionId(),
                value.lastAgentSequence(), value.lastServiceSequence(), text(value.connectedAt()),
                text(value.lastSeenAt()), text(value.disconnectedAt()), value.version());
    }

    @Override
    public void heartbeat(String agentId, String sessionId, AgentRegistration.Status status,
                          int activeRuns, int availableRuns, long lastAgentSequence, Instant lastSeenAt) {
        int changed = jdbc.update("""
                UPDATE agent_registration
                SET status = ?, active_runs = ?, available_runs = ?, last_agent_sequence = ?,
                    last_seen_at = ?, version = version + 1
                WHERE id = ? AND session_id = ? AND status != 'OFFLINE'
                """, status.name(), activeRuns, availableRuns, lastAgentSequence, text(lastSeenAt),
                agentId, sessionId);
        requireCurrentSession(changed, agentId);
    }

    @Override
    public void advanceSequence(String agentId, String sessionId, long lastAgentSequence,
                                Instant lastSeenAt) {
        int changed = jdbc.update("""
                UPDATE agent_registration
                SET last_agent_sequence = ?, last_seen_at = ?, version = version + 1
                WHERE id = ? AND session_id = ? AND status != 'OFFLINE'
                """, lastAgentSequence, text(lastSeenAt), agentId, sessionId);
        requireCurrentSession(changed, agentId);
    }

    @Override
    public void advanceServiceSequence(String agentId, String sessionId, long lastServiceSequence) {
        int changed = jdbc.update("""
                UPDATE agent_registration
                SET last_service_sequence = ?, version = version + 1
                WHERE id = ? AND session_id = ? AND status != 'OFFLINE'
                """, lastServiceSequence, agentId, sessionId);
        requireCurrentSession(changed, agentId);
    }

    @Override
    public void disconnect(String agentId, String sessionId, Instant disconnectedAt) {
        jdbc.update("""
                UPDATE agent_registration
                SET status = 'OFFLINE', active_runs = 0, available_runs = 0,
                    disconnected_at = ?, version = version + 1
                WHERE id = ? AND session_id = ? AND status != 'OFFLINE'
                """, text(disconnectedAt), agentId, sessionId);
    }

    @Override
    public int expireSilent(Instant lastSeenBefore, Instant disconnectedAt) {
        return jdbc.update("""
                UPDATE agent_registration
                SET status = 'OFFLINE', active_runs = 0, available_runs = 0,
                    disconnected_at = ?, version = version + 1
                WHERE status != 'OFFLINE' AND last_seen_at < ?
                """, text(disconnectedAt), text(lastSeenBefore));
    }

    private AgentRegistration row(ResultSet rs, int ignored) throws SQLException {
        return new AgentRegistration(rs.getString("id"),
                AgentRegistration.Status.valueOf(rs.getString("status")),
                rs.getString("agent_version"), rs.getInt("protocol_version"),
                rs.getString("os_name"), rs.getString("architecture"), rs.getString("java_version"),
                rs.getInt("maximum_runs"), rs.getInt("active_runs"), rs.getInt("available_runs"),
                providers(rs.getString("providers_json")), rs.getString("session_id"),
                rs.getLong("last_agent_sequence"), rs.getLong("last_service_sequence"),
                Instant.parse(rs.getString("connected_at")), Instant.parse(rs.getString("last_seen_at")),
                nullableInstant(rs.getString("disconnected_at")), rs.getLong("version"));
    }

    private List<AgentRegistration.Provider> providers(String value) throws SQLException {
        try {
            return List.copyOf(Arrays.asList(mapper.readValue(value, AgentRegistration.Provider[].class)));
        } catch (JsonProcessingException exception) {
            throw new SQLException("agent providers JSON is invalid", exception);
        }
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("agent providers cannot be encoded", exception);
        }
    }

    private static void requireCurrentSession(int changed, String agentId) {
        if (changed != 1) throw new IllegalStateException("agent session is no longer current: " + agentId);
    }

    private static String text(Instant value) { return value == null ? null : value.toString(); }

    private static Instant nullableInstant(String value) { return value == null ? null : Instant.parse(value); }
}
