package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.provider.spi.ProviderDescriptor;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ProviderCatalogQuery {
    private final ProviderRegistry registry;

    public ProviderCatalogQuery(ProviderRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry must not be null");
        }
        this.registry = registry;
    }

    public List<ProviderView> list() {
        return registry.providers().stream()
                .map(factory -> toView(factory.descriptor()))
                .sorted(Comparator.comparing(ProviderView::providerId)
                        .thenComparing(ProviderView::implementationVersion))
                .toList();
    }

    public Optional<ProviderView> find(String providerId, String version) {
        return registry.find(providerId, version).map(factory -> toView(factory.descriptor()));
    }

    private static ProviderView toView(ProviderDescriptor descriptor) {
        return new ProviderView(
                descriptor.providerId(),
                descriptor.displayName(),
                descriptor.implementationVersion(),
                descriptor.capabilities().stream()
                        .map(capability -> capability.name().toLowerCase(Locale.ROOT))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                descriptor.maximumConcurrency(),
                descriptor.accountSchema(),
                descriptor.messageSchema(),
                descriptor.recipientSchema());
    }
}
