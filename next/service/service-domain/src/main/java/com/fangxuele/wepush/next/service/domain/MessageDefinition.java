package com.fangxuele.wepush.next.service.domain;

import com.fangxuele.wepush.next.core.api.ProviderRef;

import java.time.Instant;

public record MessageDefinition(
        String id,
        WorkspaceId workspaceId,
        String name,
        ProviderRef provider,
        int revision,
        String schemaVersion,
        JsonDocument content,
        String contentHash,
        Status status,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public MessageDefinition {
        id = DomainChecks.text(id, "message id");
        name = DomainChecks.text(name, "message name");
        schemaVersion = DomainChecks.text(schemaVersion, "message schema version");
        contentHash = DomainChecks.text(contentHash, "message content hash");
        if (workspaceId == null || provider == null || content == null || status == null
                || revision < 1 || createdAt == null || updatedAt == null || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("message definition is incomplete");
        }
        DomainChecks.nonNegative(version, "message version");
    }

    public enum Status { ACTIVE, DISABLED, ARCHIVED }
}
