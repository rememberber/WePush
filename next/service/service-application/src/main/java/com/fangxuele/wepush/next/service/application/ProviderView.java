package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.core.api.ConfigDocument;

import java.util.Set;

public record ProviderView(
        String providerId,
        String displayName,
        String implementationVersion,
        Set<String> capabilities,
        int maximumConcurrency,
        ConfigDocument accountSchema,
        ConfigDocument messageSchema,
        ConfigDocument recipientSchema
) {
    public ProviderView {
        capabilities = Set.copyOf(capabilities);
    }
}
