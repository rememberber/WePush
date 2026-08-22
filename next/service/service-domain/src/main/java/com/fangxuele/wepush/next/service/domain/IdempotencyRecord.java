package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record IdempotencyRecord(
        WorkspaceId workspaceId,
        String scope,
        String keyHash,
        String requestHash,
        String resourceId,
        int responseStatus,
        Instant createdAt,
        Instant expiresAt
) {
    public IdempotencyRecord {
        scope = DomainChecks.text(scope, "idempotency scope");
        keyHash = DomainChecks.text(keyHash, "idempotency key hash");
        requestHash = DomainChecks.text(requestHash, "idempotency request hash");
        resourceId = DomainChecks.text(resourceId, "idempotency resource id");
        if (workspaceId == null || createdAt == null || expiresAt == null || !expiresAt.isAfter(createdAt)
                || responseStatus < 100 || responseStatus > 599) {
            throw new IllegalArgumentException("idempotency record is incomplete");
        }
    }
}
