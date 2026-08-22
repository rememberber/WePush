package com.fangxuele.wepush.next.agent.protocol;

public record ProviderCapability(
        String providerId,
        String implementationVersion,
        int spiMajorVersion,
        int maximumConcurrency
) {
    public ProviderCapability {
        if (providerId == null || providerId.isBlank()
                || implementationVersion == null || implementationVersion.isBlank()) {
            throw new IllegalArgumentException("provider identity must not be blank");
        }
        if (spiMajorVersion < 1 || maximumConcurrency < 1) {
            throw new IllegalArgumentException("provider capability values must be positive");
        }
    }
}
