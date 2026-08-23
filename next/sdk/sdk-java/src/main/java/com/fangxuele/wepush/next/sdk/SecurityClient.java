package com.fangxuele.wepush.next.sdk;

import java.time.Instant;
import java.util.List;

public final class SecurityClient {
    private final HttpTransport transport;

    SecurityClient(HttpTransport transport) {
        this.transport = transport;
    }

    public IssuedToken createToken(String name, String workspaceId, Role role, String ttl) {
        return transport.postJson(workspace(workspaceId) + "/api-tokens",
                new CreateToken(name, role.name(), ttl), null, IssuedToken.class);
    }

    public List<TokenSummary> tokens() {
        return tokens("ws_default");
    }

    public List<TokenSummary> tokens(String workspaceId) {
        return List.of(transport.getJson(
                workspace(workspaceId) + "/api-tokens", TokenSummary[].class));
    }

    public void revokeToken(String tokenId) {
        revokeToken("ws_default", tokenId);
    }

    public void revokeToken(String workspaceId, String tokenId) {
        transport.delete(workspace(workspaceId) + "/api-tokens/" + pathId(tokenId));
    }

    public EnrollmentToken createAgentEnrollmentToken(String name, String ttl) {
        return createAgentEnrollmentToken(name, "ws_default", ttl);
    }

    public EnrollmentToken createAgentEnrollmentToken(String name, String workspaceId, String ttl) {
        return transport.postJson(workspace(workspaceId) + "/agent-enrollment-tokens",
                new CreateEnrollment(name, ttl), null, EnrollmentToken.class);
    }

    private static String pathId(String value) {
        if (value == null || !value.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("resource ID contains unsupported path characters");
        }
        return value;
    }

    private static String workspace(String workspaceId) {
        return "/api/v1/workspaces/" + pathId(workspaceId);
    }

    public enum Role { VIEWER, OPERATOR, ADMIN }
    private record CreateToken(String name, String role, String ttl) {}
    private record CreateEnrollment(String name, String ttl) {}
    public record IssuedToken(String tokenId, String principalId, String token, Instant expiresAt,
                              String workspaceId, String role) {}
    public record TokenSummary(String tokenId, String principalId, String name, Instant expiresAt,
                               Instant revokedAt, Instant createdAt, Instant lastUsedAt) {}
    public record EnrollmentToken(String id, String token, Instant expiresAt) {}
}
