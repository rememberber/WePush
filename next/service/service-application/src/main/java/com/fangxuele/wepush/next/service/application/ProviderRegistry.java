package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.provider.spi.ProviderFactory;

import java.util.List;
import java.util.Optional;

public interface ProviderRegistry {
    List<ProviderFactory> providers();

    Optional<ProviderFactory> find(String providerId, String implementationVersion);
}
