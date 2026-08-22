package com.fangxuele.wepush.next.core.api;

import java.time.Instant;
import java.util.Map;

public record ItemResult(
        String runId,
        String itemId,
        int attempts,
        ItemState state,
        String providerCode,
        String diagnostic,
        String externalRequestId,
        Instant completedAt,
        Map<String, String> metadata
) {
    public ItemResult {
        runId = ApiChecks.notBlank(runId, "runId");
        itemId = ApiChecks.notBlank(itemId, "itemId");
        if (state == null || completedAt == null) {
            throw new IllegalArgumentException("state and completedAt must not be null");
        }
        if (attempts < 0 || (attempts == 0 && state != ItemState.UNSENT)) {
            throw new IllegalArgumentException("attempts must be positive unless the item was unsent");
        }
        providerCode = providerCode == null ? "" : providerCode;
        diagnostic = diagnostic == null ? "" : diagnostic;
        externalRequestId = externalRequestId == null ? "" : externalRequestId;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
