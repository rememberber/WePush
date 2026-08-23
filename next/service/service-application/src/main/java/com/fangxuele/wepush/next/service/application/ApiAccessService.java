package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.ApiAccessRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

public final class ApiAccessService {
    private final ApiAccessRepository access;
    private final WorkspaceRepository workspaces;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final Clock clock;
    private final SecureRandom random;

    public ApiAccessService(ApiAccessRepository access, WorkspaceRepository workspaces,
                            ResourceIdGenerator ids,
                            TransactionRunner transactions, Clock clock, SecureRandom random) {
        this.access = access;
        this.workspaces = workspaces;
        this.ids = ids;
        this.transactions = transactions;
        this.clock = clock;
        this.random = random;
    }

    public ApiAccessRepository.AccessIdentity authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 2048) return null;
        Instant now = clock.instant();
        ApiAccessRepository.AccessIdentity identity = access.authenticate(hash(rawToken), now).orElse(null);
        if (identity != null) transactions.required(() -> access.touchToken(identity.tokenId(), now));
        return identity;
    }

    public IssuedToken create(String name, String workspaceId, ApiAccessRepository.Role role,
                              Duration ttl) {
        if (name == null || name.isBlank() || name.length() > 120 || workspaceId == null
                || !workspaceId.matches("[A-Za-z0-9._-]{1,120}") || role == null
                || ttl == null || ttl.isNegative() || ttl.isZero()
                || ttl.compareTo(Duration.ofDays(365)) > 0) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "API_TOKEN_INVALID",
                    "API Token name, Workspace, role or TTL is invalid");
        }
        requireWorkspace(workspaceId);
        Instant now = clock.instant();
        String principalId = ids.next("principal");
        String tokenId = ids.next("api-token");
        String token = token();
        Instant expiresAt = now.plus(ttl);
        transactions.required(() -> {
            access.createPrincipal(new ApiAccessRepository.Principal(principalId, name.trim(),
                    "ACTIVE", null, now));
            access.bindRole(new ApiAccessRepository.RoleBinding(principalId, workspaceId, role, now));
            access.createToken(new ApiAccessRepository.ApiToken(tokenId, principalId, name.trim(),
                    hash(token), expiresAt, null, now, null));
        });
        return new IssuedToken(tokenId, principalId, token, expiresAt, workspaceId, role);
    }

    public void initializeBootstrap(String rawToken) {
        if (access.hasSystemAdministrator()) return;
        if (rawToken == null || rawToken.length() < 24) {
            throw new IllegalStateException(
                    "API security is enabled but WEPUSH_BOOTSTRAP_TOKEN is missing or too short");
        }
        Instant now = clock.instant();
        transactions.required(() -> {
            if (access.hasSystemAdministrator()) return;
            access.createPrincipal(new ApiAccessRepository.Principal(
                    "principal_bootstrap", "Bootstrap Administrator", "ACTIVE",
                    ApiAccessRepository.SystemRole.SYSTEM_ADMIN, now));
            access.bindRole(new ApiAccessRepository.RoleBinding("principal_bootstrap", "ws_default",
                    ApiAccessRepository.Role.ADMIN, now));
            access.createToken(new ApiAccessRepository.ApiToken("token_bootstrap",
                    "principal_bootstrap", "Bootstrap Administrator", hash(rawToken),
                    now.plus(Duration.ofDays(3650)), null, now, null));
        });
    }

    public List<ApiAccessRepository.ApiToken> list(String workspaceId) {
        String target = workspace(workspaceId);
        requireWorkspace(target);
        return access.listTokens(target);
    }

    public void revoke(String tokenId, String workspaceId) {
        if (tokenId == null || !tokenId.matches("[A-Za-z0-9._-]{1,160}")) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST,
                    "API_TOKEN_ID_INVALID", "API Token id is invalid");
        }
        String targetWorkspace = workspace(workspaceId);
        requireWorkspace(targetWorkspace);
        if (!transactions.required(() -> access.revokeToken(
                tokenId, targetWorkspace, clock.instant()))) {
            throw new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND,
                    "API_TOKEN_NOT_FOUND", "API Token was not found or is already revoked");
        }
    }

    private static String workspace(String workspaceId) {
        if (workspaceId == null || !workspaceId.matches("[A-Za-z0-9._-]{1,120}")) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST,
                    "WORKSPACE_ID_INVALID", "Workspace id is invalid");
        }
        return workspaceId;
    }

    private void requireWorkspace(String workspaceId) {
        if (workspaces.findById(new WorkspaceId(workspaceId)).isEmpty()) {
            throw new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND,
                    "WORKSPACE_NOT_FOUND", "Workspace was not found: " + workspaceId);
        }
    }

    private String token() {
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        return "wpu." + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public record IssuedToken(String tokenId, String principalId, String token,
                              Instant expiresAt, String workspaceId,
                              ApiAccessRepository.Role role) {
    }
}
