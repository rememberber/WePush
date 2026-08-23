package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.ApiAccessRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class JdbcApiAccessRepository implements ApiAccessRepository {
    private final JdbcTemplate jdbc;

    public JdbcApiAccessRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<AccessIdentity> authenticate(String tokenHash, Instant now) {
        return jdbc.query("""
                SELECT t.id token_id, t.principal_id, t.expires_at, p.name principal_name,
                       p.system_role
                FROM api_token t JOIN api_principal p ON p.id = t.principal_id
                WHERE t.token_hash = ? AND t.revoked_at IS NULL AND t.expires_at > ?
                  AND p.status = 'ACTIVE'
                """, (rs, ignored) -> new AccessIdentity(rs.getString("principal_id"),
                rs.getString("principal_name"), rs.getString("token_id"),
                Instant.parse(rs.getString("expires_at")), systemRole(rs.getString("system_role")),
                roles(rs.getString("principal_id"))),
                tokenHash, now.toString()).stream().findFirst();
    }

    private List<RoleBinding> roles(String principalId) {
        return jdbc.query("SELECT * FROM role_binding WHERE principal_id = ?",
                (rs, ignored) -> new RoleBinding(principalId, rs.getString("workspace_id"),
                        Role.valueOf(rs.getString("role")),
                        Instant.parse(rs.getString("created_at"))), principalId);
    }

    @Override
    public boolean hasSystemAdministrator() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM api_principal
                WHERE status = 'ACTIVE' AND system_role = 'SYSTEM_ADMIN'
                """, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public void createPrincipal(Principal value) {
        jdbc.update("""
                INSERT INTO api_principal(id, name, status, system_role, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, value.id(), value.name(), value.status(),
                value.systemRole() == null ? null : value.systemRole().name(),
                value.createdAt().toString());
    }

    @Override
    public void createToken(ApiToken value) {
        jdbc.update("""
                INSERT INTO api_token(id, principal_id, name, token_hash, expires_at,
                                      revoked_at, created_at, last_used_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, value.id(), value.principalId(), value.name(), value.tokenHash(),
                value.expiresAt().toString(), text(value.revokedAt()), value.createdAt().toString(),
                text(value.lastUsedAt()));
    }

    @Override
    public void bindRole(RoleBinding value) {
        jdbc.update("""
                INSERT INTO role_binding(principal_id, workspace_id, role, created_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (principal_id, workspace_id) DO UPDATE SET role = excluded.role
                """, value.principalId(), value.workspaceId(), value.role().name(),
                value.createdAt().toString());
    }

    @Override
    public void touchToken(String tokenId, Instant usedAt) {
        jdbc.update("UPDATE api_token SET last_used_at = ? WHERE id = ? AND revoked_at IS NULL",
                usedAt.toString(), tokenId);
    }

    @Override
    public List<ApiToken> listTokens(String workspaceId) {
        return jdbc.query("""
                SELECT t.id, t.principal_id, t.name, t.token_hash, t.expires_at, t.revoked_at,
                       t.created_at, t.last_used_at
                FROM api_token t
                JOIN role_binding b ON b.principal_id = t.principal_id
                JOIN api_principal p ON p.id = t.principal_id
                WHERE b.workspace_id = ? AND p.system_role IS NULL
                ORDER BY t.created_at DESC, t.id
                """, (rs, ignored) -> new ApiToken(rs.getString("id"), rs.getString("principal_id"),
                rs.getString("name"), rs.getString("token_hash"),
                Instant.parse(rs.getString("expires_at")), instant(rs.getString("revoked_at")),
                Instant.parse(rs.getString("created_at")), instant(rs.getString("last_used_at"))),
                workspaceId);
    }

    @Override
    public boolean revokeToken(String tokenId, String workspaceId, Instant revokedAt) {
        return jdbc.update("""
                UPDATE api_token SET revoked_at = ?
                WHERE id = ? AND revoked_at IS NULL AND EXISTS (
                    SELECT 1 FROM role_binding b
                    WHERE b.principal_id = api_token.principal_id AND b.workspace_id = ?
                ) AND EXISTS (
                    SELECT 1 FROM api_principal p
                    WHERE p.id = api_token.principal_id AND p.system_role IS NULL
                )
                """, revokedAt.toString(), tokenId, workspaceId) == 1;
    }

    private static SystemRole systemRole(String value) {
        return value == null ? null : SystemRole.valueOf(value);
    }

    private static String text(Instant value) {
        return value == null ? null : value.toString();
    }

    private static Instant instant(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
