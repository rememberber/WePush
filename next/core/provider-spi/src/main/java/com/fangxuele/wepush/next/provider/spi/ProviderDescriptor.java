package com.fangxuele.wepush.next.provider.spi;

import com.fangxuele.wepush.next.core.api.ConfigDocument;

import java.time.Duration;
import java.util.Set;

public record ProviderDescriptor(
        String providerId,
        String displayName,
        String implementationVersion,
        int spiMajorVersion,
        Set<Capability> capabilities,
        ThreadSafetyMode threadSafetyMode,
        int maximumConcurrency,
        Duration defaultTimeout,
        ConfigDocument accountSchema,
        ConfigDocument messageSchema,
        ConfigDocument recipientSchema
) {
    public ProviderDescriptor {
        if (providerId == null || providerId.isBlank()
                || displayName == null || displayName.isBlank()
                || implementationVersion == null || implementationVersion.isBlank()) {
            throw new IllegalArgumentException("provider identity is required");
        }
        if (spiMajorVersion < 1 || maximumConcurrency < 1) {
            throw new IllegalArgumentException("SPI version and maximum concurrency must be positive");
        }
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
        if (threadSafetyMode == null || defaultTimeout == null || defaultTimeout.isNegative()
                || defaultTimeout.isZero() || accountSchema == null || messageSchema == null
                || recipientSchema == null) {
            throw new IllegalArgumentException("provider descriptor is incomplete");
        }
    }

    public enum Capability {
        PREVIEW,
        DRY_RUN,
        IDEMPOTENCY,
        RESPONSE_BODY
    }

    public enum ThreadSafetyMode {
        THREAD_SAFE,
        SERIALIZED,
        SESSION_PER_WORKER
    }
}
