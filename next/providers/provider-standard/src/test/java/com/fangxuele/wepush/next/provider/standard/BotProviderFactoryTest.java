package com.fangxuele.wepush.next.provider.standard;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.InMemorySecretValue;
import com.fangxuele.wepush.next.core.api.ItemState;
import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RecipientValue;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.core.api.SecretRef;
import com.fangxuele.wepush.next.core.api.SecretResolver;
import com.fangxuele.wepush.next.provider.spi.ErrorCategory;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.provider.spi.ProviderOpenContext;
import com.fangxuele.wepush.next.provider.spi.ProviderResult;
import com.fangxuele.wepush.next.provider.spi.ProviderSendRequest;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotProviderFactoryTest {
    @Test
    void sendsSignedFeishuTextAndMapsSuccess() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        try (MockBotEndpoint endpoint = new MockBotEndpoint(requestBody, "{\"code\":0,\"msg\":\"success\"}")) {
            ProviderResult result = send(new FeishuBotProviderFactory(true), endpoint.uri(),
                    "{\"type\":\"TEXT\",\"contentTemplate\":\"hello {{name}}\",\"mentionField\":\"openId\"}",
                    Map.of("name", "Alice", "openId", "ou_test"), true);

            assertEquals(ItemState.SUCCEEDED, result.outcome());
            var payload = StandardProviderSupport.JSON.readTree(requestBody.get());
            assertEquals("text", payload.path("msg_type").asText());
            assertTrue(payload.path("content").path("text").asText().contains("ou_test"));
            assertFalse(payload.path("timestamp").asText().isBlank());
            assertFalse(payload.path("sign").asText().isBlank());
        }
    }

    @Test
    void sendsSignedDingTalkMarkdown() throws Exception {
        AtomicReference<String> requestUri = new AtomicReference<>();
        try (MockBotEndpoint endpoint = new MockBotEndpoint(new AtomicReference<>(),
                "{\"errcode\":0,\"errmsg\":\"ok\"}", requestUri)) {
            ProviderResult result = send(new DingTalkBotProviderFactory(true), endpoint.uri(),
                    "{\"type\":\"MARKDOWN\",\"titleTemplate\":\"Build {{name}}\",\"contentTemplate\":\"done\"}",
                    Map.of("name", "A"), true);

            assertEquals(ItemState.SUCCEEDED, result.outcome());
            assertTrue(requestUri.get().contains("timestamp="));
            assertTrue(requestUri.get().contains("sign="));
        }
    }

    @Test
    void sendsWeComTextAndClassifiesBusinessRateLimit() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        try (MockBotEndpoint endpoint = new MockBotEndpoint(body,
                "{\"errcode\":45009,\"errmsg\":\"api frequency limit\"}")) {
            ProviderResult result = send(new WeComBotProviderFactory(true), endpoint.uri(),
                    "{\"type\":\"TEXT\",\"contentTemplate\":\"hello {{name}}\"}",
                    Map.of("name", "Alice"), false);

            assertEquals(ItemState.FAILED, result.outcome());
            assertEquals(ErrorCategory.RATE_LIMITED, result.category());
            assertTrue(result.retryable());
            assertEquals("text", StandardProviderSupport.JSON.readTree(body.get()).path("msgtype").asText());
        }
    }

    @Test
    void dryRunBuildsPayloadWithoutResolvingWebhookSecret() throws Exception {
        FeishuBotProviderFactory factory = new FeishuBotProviderFactory();
        ConfigDocument account = account(false);
        ConfigDocument message = json("message", """
                {"type":"TEXT","contentTemplate":"hello {{name}}"}
                """);
        ProviderResult result = send(factory, account, message, recipient(Map.of("name", "Alice")),
                ref -> { throw new AssertionError("Dry Run must not resolve secrets"); }, true);

        assertEquals("DRY_RUN", result.code());
    }

    @Test
    void rejectsIncompleteBotMessageConfiguration() {
        var result = new DingTalkBotProviderFactory().validateMessage(json("message", """
                {"type":"ACTION_CARD","titleTemplate":"title","contentTemplate":"content"}
                """));

        assertFalse(result.validResult());
        assertEquals("BUTTON_REQUIRED", result.violations().getFirst().code());
    }

    @Test
    void treatsMalformedResponseAsUnknownAndRejectsNonDefaultOfficialPort() throws Exception {
        try (MockBotEndpoint endpoint = new MockBotEndpoint(new AtomicReference<>(), "not-json")) {
            ProviderResult result = send(new WeComBotProviderFactory(true), endpoint.uri(),
                    "{\"type\":\"TEXT\",\"contentTemplate\":\"hello\"}", Map.of(), false);
            assertEquals(ItemState.UNKNOWN, result.outcome());
            assertEquals("BOT_RESPONSE_INVALID", result.code());
        }
        boolean rejected = false;
        try {
            BotVendor.FEISHU.validateEndpoint(URI.create(
                    "https://open.feishu.cn:444/open-apis/bot/v2/hook/test"), false);
        } catch (ProviderConfigException expected) { rejected = true; }
        assertTrue(rejected);
    }

    private ProviderResult send(ProviderFactory factory, URI webhook, String message,
                                Map<String, String> fields, boolean signing) throws Exception {
        SecretResolver resolver = ref -> InMemorySecretValue.of(ref.name().equals("webhook")
                ? webhook.toString() : "signing-secret");
        return send(factory, account(signing), json("message", message), recipient(fields), resolver, false);
    }

    private ProviderResult send(ProviderFactory factory, ConfigDocument account, ConfigDocument message,
                                RecipientRecord recipient, SecretResolver resolver, boolean dryRun) throws Exception {
        RunExecutionSpec spec = new RunExecutionSpec("run-bot",
                new ProviderRef(factory.descriptor().providerId(), factory.descriptor().implementationVersion()),
                account, message, ExecutionPolicies.defaults(), Map.of(), dryRun, Instant.now());
        try (ProviderSession session = factory.open(new ProviderOpenContext(spec, resolver, ExecutionClock.system()))) {
            return session.send(new ProviderSendRequest(spec.runId(), recipient.itemId(), 1, recipient,
                    message, "bot-key", Instant.now().plusSeconds(5)), () -> false);
        }
    }

    private static ConfigDocument account(boolean signing) {
        return json("account", signing ? """
                {"webhook":{"namespace":"bot","name":"webhook","version":"v1"},
                 "signingSecret":{"namespace":"bot","name":"signing","version":"v1"}}
                """ : """
                {"webhook":{"namespace":"bot","name":"webhook","version":"v1"}}
                """);
    }

    private static RecipientRecord recipient(Map<String, String> fields) {
        Map<String, RecipientValue> values = new java.util.LinkedHashMap<>();
        fields.forEach((name, value) -> values.put(name, new RecipientValue.TextValue(value)));
        return new RecipientRecord("bot-1", 0, values);
    }

    private static ConfigDocument json(String schema, String value) {
        return new ConfigDocument(schema, "1", value.getBytes(StandardCharsets.UTF_8));
    }

    private static final class MockBotEndpoint implements AutoCloseable {
        private final HttpServer server;

        private MockBotEndpoint(AtomicReference<String> requestBody, String response) throws Exception {
            this(requestBody, response, new AtomicReference<>());
        }

        private MockBotEndpoint(AtomicReference<String> requestBody, String response,
                                AtomicReference<String> requestUri) throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/hook", exchange -> {
                requestUri.set(exchange.getRequestURI().toString());
                requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
        }

        URI uri() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/hook"); }

        @Override
        public void close() { server.stop(0); }
    }
}
