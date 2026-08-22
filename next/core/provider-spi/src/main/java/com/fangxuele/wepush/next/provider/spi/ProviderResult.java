package com.fangxuele.wepush.next.provider.spi;

import com.fangxuele.wepush.next.core.api.ItemState;

import java.time.Duration;
import java.util.Map;

public record ProviderResult(
        ItemState outcome,
        String code,
        ErrorCategory category,
        boolean retryable,
        Duration retryAfter,
        String diagnostic,
        String externalRequestId,
        Map<String, String> metadata
) {
    public ProviderResult {
        if (outcome == null || outcome == ItemState.UNSENT || category == null) {
            throw new IllegalArgumentException("provider outcome is invalid");
        }
        code = code == null ? "" : code;
        diagnostic = diagnostic == null ? "" : diagnostic;
        externalRequestId = externalRequestId == null ? "" : externalRequestId;
        if (retryAfter != null && retryAfter.isNegative()) {
            throw new IllegalArgumentException("retryAfter must be non-negative");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static ProviderResult success(String code, String externalRequestId) {
        return new ProviderResult(ItemState.SUCCEEDED, code, ErrorCategory.NONE, false,
                null, "", externalRequestId, Map.of());
    }

    public static ProviderResult failure(
            String code,
            ErrorCategory category,
            boolean retryable,
            String diagnostic
    ) {
        return new ProviderResult(ItemState.FAILED, code, category, retryable,
                null, diagnostic, "", Map.of());
    }
}
