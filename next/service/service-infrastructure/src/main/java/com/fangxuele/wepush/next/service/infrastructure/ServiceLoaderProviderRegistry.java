package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.service.application.ProviderRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public final class ServiceLoaderProviderRegistry implements ProviderRegistry {
    private final List<ProviderFactory> providers;
    private final Map<ProviderKey, ProviderFactory> byKey;

    public ServiceLoaderProviderRegistry(ClassLoader classLoader) {
        this(ServiceLoader.load(ProviderFactory.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)
                .toList());
    }

    public ServiceLoaderProviderRegistry(List<? extends ProviderFactory> factories) {
        if (factories == null) {
            throw new IllegalArgumentException("factories must not be null");
        }
        Map<ProviderKey, ProviderFactory> discovered = new LinkedHashMap<>();
        for (ProviderFactory factory : factories) {
            ProviderKey key = new ProviderKey(
                    factory.descriptor().providerId(), factory.descriptor().implementationVersion());
            if (discovered.putIfAbsent(key, factory) != null) {
                throw new IllegalStateException("Duplicate Provider: " + key.providerId + "@" + key.version);
            }
        }
        byKey = Map.copyOf(discovered);
        providers = List.copyOf(discovered.values());
    }

    @Override
    public List<ProviderFactory> providers() {
        return providers;
    }

    @Override
    public Optional<ProviderFactory> find(String providerId, String implementationVersion) {
        return Optional.ofNullable(byKey.get(new ProviderKey(providerId, implementationVersion)));
    }

    private record ProviderKey(String providerId, String version) {
    }
}
