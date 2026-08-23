package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.Optional;

public interface AgentIdentityRepository {
    void createEnrollment(EnrollmentToken token);

    boolean consumeEnrollment(String id, String tokenHash, Instant now);

    void createCredential(AgentCredential credential);

    Optional<AgentCredential> findCredential(String credentialId);

    boolean revokeCredential(String credentialId, String agentId, Instant revokedAt);

    void touchCredential(String credentialId, Instant usedAt);

    Optional<String> enrollmentWorkspace(String enrollmentId);

    void bindWorkspace(String agentId, String workspaceId, Instant createdAt);

    boolean allowedInWorkspace(String agentId, String workspaceId);

    record EnrollmentToken(String id, String name, String tokenHash, String workspaceId, Instant expiresAt,
                           Instant usedAt, Instant createdAt) {
        public EnrollmentToken {
            if (id == null || id.isBlank() || name == null || name.isBlank()
                    || tokenHash == null || tokenHash.length() != 64 || workspaceId == null
                    || workspaceId.isBlank() || expiresAt == null
                    || createdAt == null || !expiresAt.isAfter(createdAt)) {
                throw new IllegalArgumentException("Agent Enrollment Token is incomplete");
            }
        }
    }

    record AgentCredential(String id, String agentId, String tokenHash,
                           String certificateFingerprint, Instant expiresAt, Instant revokedAt,
                           Instant createdAt, Instant lastUsedAt) {
        public AgentCredential {
            if (id == null || id.isBlank() || agentId == null || agentId.isBlank()
                    || tokenHash == null || tokenHash.length() != 64 || expiresAt == null
                    || createdAt == null || !expiresAt.isAfter(createdAt)) {
                throw new IllegalArgumentException("Agent Credential is incomplete");
            }
            certificateFingerprint = certificateFingerprint == null ? "" : certificateFingerprint;
        }

        public boolean activeAt(Instant now) {
            return revokedAt == null && expiresAt.isAfter(now);
        }
    }
}
