package com.fangxuele.wepush.next.integration;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RecipientValue;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.core.api.RunState;
import com.fangxuele.wepush.next.embedded.InMemoryExecutionStore;
import com.fangxuele.wepush.next.embedded.WePushEngine;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardProviderEngineIntegrationTest {
    private static final String SECRET = """
            {"namespace":"integration","name":"not-resolved-in-dry-run","version":"v1"}
            """.trim();

    @Test
    void discoversAndExecutesEveryBuiltInStandardProviderThroughTheEngine() throws Exception {
        List<ProviderFactory> standard = ServiceLoader.load(ProviderFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(provider -> !provider.descriptor().providerId().equals("wepush.http"))
                .toList();
        assertEquals(8, standard.size());
        assertTrue(standard.stream().allMatch(provider ->
                provider.descriptor().capabilities().stream().anyMatch(
                        capability -> capability.name().equals("DRY_RUN"))));

        InMemoryExecutionStore store = new InMemoryExecutionStore();
        var builder = WePushEngine.builder()
                .secretResolver(ref -> { throw new AssertionError("Dry Run resolved a SecretRef"); })
                .resultSink(store)
                .eventSink(store);
        standard.forEach(builder::provider);

        try (WePushEngine engine = builder.build()) {
            for (ProviderFactory provider : standard) {
                Fixture fixture = fixture(provider.descriptor().providerId());
                String runId = "engine-" + provider.descriptor().providerId().replace('.', '-');
                var summary = engine.start(new RunExecutionSpec(runId,
                                new ProviderRef(provider.descriptor().providerId(),
                                        provider.descriptor().implementationVersion()),
                                json("account", fixture.account()), json("message", fixture.message()),
                                ExecutionPolicies.defaults(), Map.of("test", "alpha.4"), true, Instant.now()),
                                List.of(recipient(fixture.recipient())))
                        .completion().toCompletableFuture().get(10, TimeUnit.SECONDS);

                assertEquals(RunState.SUCCEEDED, summary.finalState(), provider.descriptor().providerId());
                assertEquals(1, summary.succeeded(), provider.descriptor().providerId());
            }
        }
        assertEquals(8, store.results().size());
        assertTrue(store.results().stream().allMatch(result -> result.providerCode().equals("DRY_RUN")));
    }

    private static Fixture fixture(String providerId) {
        return switch (providerId) {
            case "wepush.email.smtp" -> new Fixture(
                    "{\"host\":\"smtp.example.com\",\"port\":587,\"security\":\"STARTTLS\",\"fromAddress\":\"sender@example.com\"}",
                    "{\"subjectTemplate\":\"Hello {{name}}\",\"textBodyTemplate\":\"Welcome {{name}}\"}",
                    Map.of("email", "alice@example.com", "name", "Alice"));
            case "wepush.bot.feishu", "wepush.bot.dingtalk", "wepush.bot.wecom" -> new Fixture(
                    "{\"webhook\":" + SECRET + "}",
                    "{\"type\":\"TEXT\",\"contentTemplate\":\"Hello {{name}}\"}",
                    Map.of("name", "Alice"));
            case "wepush.sms.aliyun" -> new Fixture(
                    "{\"accessKeyId\":\"test-id\",\"accessKeySecret\":" + SECRET + "}",
                    "{\"signName\":\"WePush\",\"templateCode\":\"SMS_123\",\"templateParamJsonTemplate\":\"{\\\"name\\\":\\\"{{name}}\\\"}\"}",
                    Map.of("phoneNumber", "13800138000", "name", "Alice"));
            case "wepush.wechat.official" -> new Fixture(
                    "{\"appId\":\"app-id\",\"appSecret\":" + SECRET + "}",
                    "{\"type\":\"TEMPLATE\",\"payloadJsonTemplate\":\"{\\\"template_id\\\":\\\"tpl\\\",\\\"data\\\":{\\\"name\\\":{\\\"value\\\":\\\"{{name}}\\\"}}}\"}",
                    Map.of("openId", "openid-1", "name", "Alice"));
            case "wepush.wechat.mini" -> new Fixture(
                    "{\"appId\":\"mini-id\",\"appSecret\":" + SECRET + "}",
                    "{\"type\":\"SUBSCRIBE\",\"payloadJsonTemplate\":\"{\\\"template_id\\\":\\\"tpl\\\",\\\"data\\\":{}}\"}",
                    Map.of("openId", "openid-1", "name", "Alice"));
            case "wepush.wecom.app" -> new Fixture(
                    "{\"corpId\":\"corp-id\",\"corpSecret\":" + SECRET + ",\"agentId\":100001}",
                    "{\"type\":\"APP\",\"payloadJsonTemplate\":\"{\\\"msgtype\\\":\\\"text\\\",\\\"text\\\":{\\\"content\\\":\\\"Hello {{name}}\\\"}}\"}",
                    Map.of("userId", "alice", "name", "Alice"));
            default -> throw new IllegalArgumentException("Missing fixture for " + providerId);
        };
    }

    private static RecipientRecord recipient(Map<String, String> fields) {
        return new RecipientRecord("recipient-1", 0, fields.entrySet().stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        entry -> new RecipientValue.TextValue(entry.getValue()))));
    }

    private static ConfigDocument json(String schema, String value) {
        return new ConfigDocument(schema, "1", value.getBytes(StandardCharsets.UTF_8));
    }

    private record Fixture(String account, String message, Map<String, String> recipient) { }
}
