package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record Workspace(
        WorkspaceId id,
        String name,
        Status status,
        Instant createdAt,
        long version
) {
    public Workspace {
        if (id == null || name == null || name.isBlank() || status == null || createdAt == null) {
            throw new IllegalArgumentException("workspace is incomplete");
        }
        if (version < 0) {
            throw new IllegalArgumentException("workspace version must be non-negative");
        }
    }

    public enum Status {
        ACTIVE,
        SUSPENDED,
        ARCHIVED
    }
}
