package com.fangxuele.wepush.next.service.domain;

import com.fangxuele.wepush.next.core.api.ProviderRef;

import java.time.Instant;

public record AccountDefinition(
        String id,
        WorkspaceId workspaceId,
        String name,
        ProviderRef provider,
        JsonDocument configuration,
        Status status,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public AccountDefinition {
        id = DomainChecks.text(id, "account id");
        name = DomainChecks.text(name, "account name");
        if (workspaceId == null || provider == null || configuration == null || status == null
                || createdAt == null || updatedAt == null || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("account definition is incomplete");
        }
        DomainChecks.nonNegative(version, "account version");
    }

    public enum Status { ACTIVE, DISABLED, ARCHIVED }
}
