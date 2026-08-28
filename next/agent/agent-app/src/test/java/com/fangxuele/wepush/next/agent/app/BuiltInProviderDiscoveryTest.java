package com.fangxuele.wepush.next.agent.app;

import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuiltInProviderDiscoveryTest {
    @Test
    void discoversAllBuiltInProviderVersionsUsedByAgentCapabilities() {
        Set<String> providers = ServiceLoader.load(ProviderFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .map(factory -> factory.descriptor().providerId())
                .collect(Collectors.toSet());

        assertEquals(Set.of("wepush.http", "wepush.email.smtp", "wepush.bot.feishu",
                "wepush.bot.dingtalk", "wepush.bot.wecom", "wepush.sms.aliyun",
                "wepush.wechat.official", "wepush.wechat.mini", "wepush.wecom.app"), providers);
    }
}
