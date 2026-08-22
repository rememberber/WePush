package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.core.api.SecretRef;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;

import java.time.Instant;

public record SecretMetadata(
        WorkspaceId workspaceId,
        SecretRef ref,
        boolean configured,
        long recordVersion,
        Instant createdAt,
        Instant updatedAt
) {
    public SecretMetadata {
        if (workspaceId == null || ref == null || recordVersion < 1 || createdAt == null || updatedAt == null
                || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("secret metadata is incomplete");
        }
    }
}
