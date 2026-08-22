package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record AudienceDefinition(
        String id,
        WorkspaceId workspaceId,
        String name,
        String snapshotId,
        int revision,
        long recordCount,
        String contentHash,
        Status status,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public AudienceDefinition {
        id = DomainChecks.text(id, "audience id");
        name = DomainChecks.text(name, "audience name");
        snapshotId = DomainChecks.text(snapshotId, "audience snapshot id");
        contentHash = DomainChecks.text(contentHash, "audience content hash");
        if (workspaceId == null || status == null || revision < 1
                || createdAt == null || updatedAt == null || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("audience definition is incomplete");
        }
        DomainChecks.nonNegative(recordCount, "audience record count");
        DomainChecks.nonNegative(version, "audience version");
    }

    public enum Status { ACTIVE, DISABLED, ARCHIVED }
}
