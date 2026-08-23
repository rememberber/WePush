package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.AgentIdentityRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Optional;

public final class JdbcAgentIdentityRepository implements AgentIdentityRepository {
    private final JdbcTemplate jdbc;

    public JdbcAgentIdentityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void createEnrollment(EnrollmentToken value) {
        jdbc.update("""
                INSERT INTO agent_enrollment_token
                (id, name, token_hash, workspace_id, expires_at, used_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, value.id(), value.name(), value.tokenHash(), value.workspaceId(), text(value.expiresAt()),
                text(value.usedAt()), text(value.createdAt()));
    }

    @Override
    public boolean consumeEnrollment(String id, String tokenHash, Instant now) {
        return jdbc.update("""
                UPDATE agent_enrollment_token SET used_at = ?
                WHERE id = ? AND token_hash = ? AND used_at IS NULL AND expires_at > ?
                """, text(now), id, tokenHash, text(now)) == 1;
    }

    @Override
    public void createCredential(AgentCredential value) {
        jdbc.update("""
                INSERT INTO agent_credential
                (id, agent_id, token_hash, certificate_fingerprint, expires_at,
                 revoked_at, created_at, last_used_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, value.id(), value.agentId(), value.tokenHash(), value.certificateFingerprint(),
                text(value.expiresAt()), text(value.revokedAt()), text(value.createdAt()),
                text(value.lastUsedAt()));
    }

    @Override
    public Optional<AgentCredential> findCredential(String credentialId) {
        return jdbc.query("SELECT * FROM agent_credential WHERE id = ?", (rs, ignored) ->
                new AgentCredential(rs.getString("id"), rs.getString("agent_id"),
                        rs.getString("token_hash"), rs.getString("certificate_fingerprint"),
                        Instant.parse(rs.getString("expires_at")), instant(rs.getString("revoked_at")),
                        Instant.parse(rs.getString("created_at")), instant(rs.getString("last_used_at"))),
                credentialId).stream().findFirst();
    }

    @Override
    public boolean revokeCredential(String credentialId, String agentId, Instant revokedAt) {
        return jdbc.update("""
                UPDATE agent_credential SET revoked_at = ?
                WHERE id = ? AND agent_id = ? AND revoked_at IS NULL
                """, text(revokedAt), credentialId, agentId) == 1;
    }

    @Override
    public void touchCredential(String credentialId, Instant usedAt) {
        jdbc.update("UPDATE agent_credential SET last_used_at = ? WHERE id = ? AND revoked_at IS NULL",
                text(usedAt), credentialId);
    }

    @Override
    public Optional<String> enrollmentWorkspace(String enrollmentId) {
        return jdbc.query("SELECT workspace_id FROM agent_enrollment_token WHERE id = ?",
                (rs, ignored) -> rs.getString(1), enrollmentId).stream().findFirst();
    }

    @Override
    public void bindWorkspace(String agentId, String workspaceId, Instant createdAt) {
        jdbc.update("""
                INSERT INTO agent_workspace_binding(agent_id, workspace_id, created_at)
                VALUES (?, ?, ?) ON CONFLICT (agent_id, workspace_id) DO NOTHING
                """, agentId, workspaceId, text(createdAt));
    }

    @Override
    public boolean allowedInWorkspace(String agentId, String workspaceId) {
        Integer matched = jdbc.queryForObject("""
                SELECT COUNT(*) FROM agent_workspace_binding WHERE agent_id = ? AND workspace_id = ?
                """, Integer.class, agentId, workspaceId);
        if (matched != null && matched > 0) return true;
        Integer bindings = jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_workspace_binding WHERE agent_id = ?", Integer.class, agentId);
        return "ws_default".equals(workspaceId) && (bindings == null || bindings == 0);
    }

    private static String text(Instant value) {
        return value == null ? null : value.toString();
    }

    private static Instant instant(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
