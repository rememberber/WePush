package com.fangxuele.wepush.next.provider.spi;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.RecipientRecord;

import java.time.Instant;

public record ProviderSendRequest(
        String runId,
        String itemId,
        int attempt,
        RecipientRecord recipient,
        ConfigDocument messageConfig,
        String idempotencyKey,
        Instant deadline
) {
    public ProviderSendRequest {
        if (runId == null || runId.isBlank() || itemId == null || itemId.isBlank()
                || idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("provider request identity is required");
        }
        if (attempt < 1 || recipient == null || messageConfig == null || deadline == null) {
            throw new IllegalArgumentException("provider request is incomplete");
        }
    }
}
