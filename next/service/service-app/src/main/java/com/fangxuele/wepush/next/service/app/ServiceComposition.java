package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.ProviderCatalogQuery;
import com.fangxuele.wepush.next.service.application.ProviderRegistry;
import com.fangxuele.wepush.next.service.infrastructure.ServiceLoaderProviderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ServiceComposition {
    @Bean
    ProviderRegistry providerRegistry() {
        return new ServiceLoaderProviderRegistry(Thread.currentThread().getContextClassLoader());
    }

    @Bean
    ProviderCatalogQuery providerCatalogQuery(ProviderRegistry registry) {
        return new ProviderCatalogQuery(registry);
    }
}
