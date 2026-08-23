package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ApiAccessRepository {
    Optional<AccessIdentity> authenticate(String tokenHash, Instant now);

    boolean hasSystemAdministrator();

    void createPrincipal(Principal principal);

    void createToken(ApiToken token);

    void bindRole(RoleBinding binding);

    void touchToken(String tokenId, Instant usedAt);

    List<ApiToken> listTokens(String workspaceId);

    boolean revokeToken(String tokenId, String workspaceId, Instant revokedAt);

    record AccessIdentity(String principalId, String principalName, String tokenId,
                          Instant expiresAt, SystemRole systemRole, List<RoleBinding> roles) {
        public AccessIdentity {
            roles = List.copyOf(roles);
        }

        public Role roleFor(String workspaceId) {
            return roles.stream().filter(binding -> binding.workspaceId().equals(workspaceId))
                    .map(RoleBinding::role).findFirst().orElse(null);
        }

        public boolean systemAdministrator() {
            return systemRole == SystemRole.SYSTEM_ADMIN;
        }
    }

    record Principal(String id, String name, String status, SystemRole systemRole,
                     Instant createdAt) {
    }

    record ApiToken(String id, String principalId, String name, String tokenHash,
                    Instant expiresAt, Instant revokedAt, Instant createdAt, Instant lastUsedAt) {
    }

    record RoleBinding(String principalId, String workspaceId, Role role, Instant createdAt) {
    }

    enum Role {
        VIEWER,
        OPERATOR,
        ADMIN;

        public boolean grants(Role required) {
            return ordinal() >= required.ordinal();
        }
    }

    enum SystemRole {
        SYSTEM_ADMIN
    }
}
