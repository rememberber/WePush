package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record MessageRevision(String messageId, WorkspaceId workspaceId, int revision,
                              String schemaVersion, JsonDocument content, String contentHash,
                              Instant createdAt) {
    public MessageRevision {
        messageId = DomainChecks.text(messageId, "message id");
        schemaVersion = DomainChecks.text(schemaVersion, "message schema version");
        contentHash = DomainChecks.text(contentHash, "message content hash");
        if (workspaceId == null || revision < 1 || content == null || createdAt == null) {
            throw new IllegalArgumentException("message revision is incomplete");
        }
    }
}
