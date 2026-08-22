package com.fangxuele.wepush.next.service.api;

import java.util.Set;

public record ProviderSummaryResponse(
        String providerId,
        String displayName,
        String implementationVersion,
        Set<String> capabilities,
        int maximumConcurrency,
        Links links
) {
    public ProviderSummaryResponse {
        capabilities = Set.copyOf(capabilities);
    }

    public record Links(String accountSchema, String messageSchema, String recipientSchema) {
    }
}
