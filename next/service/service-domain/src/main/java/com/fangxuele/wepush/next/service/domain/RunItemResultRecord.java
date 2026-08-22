package com.fangxuele.wepush.next.service.domain;

import com.fangxuele.wepush.next.core.api.ItemState;

import java.time.Instant;

public record RunItemResultRecord(
        String runId,
        WorkspaceId workspaceId,
        String itemId,
        int attempts,
        ItemState state,
        String providerCode,
        String diagnostic,
        String externalRequestId,
        Instant completedAt,
        JsonDocument metadata
) {
    public RunItemResultRecord {
        runId = DomainChecks.text(runId, "result run id");
        itemId = DomainChecks.text(itemId, "result item id");
        providerCode = providerCode == null ? "" : providerCode;
        diagnostic = diagnostic == null ? "" : diagnostic;
        externalRequestId = externalRequestId == null ? "" : externalRequestId;
        if (workspaceId == null || state == null || completedAt == null || metadata == null) {
            throw new IllegalArgumentException("run item result is incomplete");
        }
        if (attempts < 0 || (attempts == 0 && state != ItemState.UNSENT)) {
            throw new IllegalArgumentException("result attempts are invalid");
        }
    }
}
