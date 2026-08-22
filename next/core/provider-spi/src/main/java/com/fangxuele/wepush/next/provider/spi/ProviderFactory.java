package com.fangxuele.wepush.next.provider.spi;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.SecretResolver;

import java.time.Duration;

public interface ProviderFactory {
    ProviderDescriptor descriptor();

    ValidationResult validateAccount(ConfigDocument account);

    ValidationResult validateMessage(ConfigDocument message);

    ConnectionTestResult testConnection(
            ConfigDocument account,
            SecretResolver secrets,
            Duration timeout
    );

    ProviderSession open(ProviderOpenContext context) throws Exception;
}
