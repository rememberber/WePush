package com.fangxuele.wepush.next.provider.standard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.InMemorySecretValue;
import com.fangxuele.wepush.next.core.api.ItemState;
import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RecipientValue;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.core.api.SecretResolver;
import com.fangxuele.wepush.next.provider.spi.ErrorCategory;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.provider.spi.ProviderOpenContext;
import com.fangxuele.wepush.next.provider.spi.ProviderResult;
import com.fangxuele.wepush.next.provider.spi.ProviderSendRequest;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeChatProviderFactoryTest {
    @Test
    void sendsOfficialAccountTemplateWithCachedToken() throws Exception {
        try (MockWeChatApi api = new MockWeChatApi()) {
            api.sendResponses.add("{\"errcode\":0,\"errmsg\":\"ok\",\"msgid\":\"msg-1\"}");
            ProviderFactory factory = new WeChatOfficialProviderFactory(api.base());
            ConfigDocument message = officialMessage();
            RunExecutionSpec spec = new RunExecutionSpec("run-wechat-cache",
                    new ProviderRef(factory.descriptor().providerId(), factory.descriptor().implementationVersion()),
                    officialAccount(), message, ExecutionPolicies.defaults(), Map.of(), false, Instant.now());
            ProviderResult first;
            ProviderResult second;
            try (ProviderSession session = factory.open(new ProviderOpenContext(spec, secrets(),
                    ExecutionClock.system()))) {
                first = session.send(request(spec, message,
                        recipient("openId", "openid-1"), "recipient-1"), () -> false);
                second = session.send(request(spec, message,
                        recipient("openId", "openid-2"), "recipient-2"), () -> false);
            }

            assertEquals(ItemState.SUCCEEDED, first.outcome());
            assertEquals("msg-1", first.externalRequestId());
            assertEquals(ItemState.SUCCEEDED, second.outcome());
            assertEquals(1, api.tokenCalls.get());
            assertEquals("/cgi-bin/message/template/send", api.sendPaths.getFirst());
            JsonNode payload = StandardProviderSupport.JSON.readTree(api.requestBodies.getFirst());
            assertEquals("openid-1", payload.path("touser").asText());
            assertEquals("Alice", payload.path("data").path("name").path("value").asText());
        }
    }

    @Test
    void refreshesRejectedTokenExactlyOnceBeforeSending() throws Exception {
        try (MockWeChatApi api = new MockWeChatApi()) {
            api.sendResponses.add("{\"errcode\":40014,\"errmsg\":\"invalid token\"}");
            api.sendResponses.add("{\"errcode\":0,\"errmsg\":\"ok\",\"msgid\":\"msg-2\"}");
            ProviderResult result = send(new WeChatMiniProviderFactory(api.base()), miniAccount(),
                    miniMessage(), recipient("openId", "openid-mini"), false);

            assertEquals(ItemState.SUCCEEDED, result.outcome());
            assertEquals(2, api.tokenCalls.get());
            assertEquals(2, api.sendCalls.get());
            assertEquals("/cgi-bin/message/subscribe/send", api.sendPaths.getFirst());
            assertTrue(api.sendQueries.get(0).contains("access_token=token-1"));
            assertTrue(api.sendQueries.get(1).contains("access_token=token-2"));
        }
    }

    @Test
    void sendsWeComApplicationPayloadWithManagedEnvelope() throws Exception {
        try (MockWeChatApi api = new MockWeChatApi()) {
            api.sendResponses.add("{\"errcode\":0,\"errmsg\":\"ok\",\"msgid\":\"wc-1\"}");
            ProviderResult result = send(new WeComAppProviderFactory(api.base()), weComAccount(),
                    weComMessage(), recipient("partyId", "42"), false);

            assertEquals(ItemState.SUCCEEDED, result.outcome());
            assertTrue(api.tokenQueries.getFirst().contains("corpid=corp-id"));
            assertEquals("/cgi-bin/message/send", api.sendPaths.getFirst());
            JsonNode payload = StandardProviderSupport.JSON.readTree(api.requestBodies.getFirst());
            assertEquals("42", payload.path("toparty").asText());
            assertEquals(100001, payload.path("agentid").asInt());
            assertEquals(1, payload.path("enable_duplicate_check").asInt());
            assertEquals("Hello Alice", payload.path("text").path("content").asText());
        }
    }

    @Test
    void dryRunBuildsEnvelopeWithoutResolvingAppSecret() throws Exception {
        AtomicInteger resolutions = new AtomicInteger();
        SecretResolver resolver = ref -> {
            resolutions.incrementAndGet();
            throw new AssertionError("Dry Run must not resolve secrets");
        };
        ProviderResult result = send(new WeChatOfficialProviderFactory(), officialAccount(),
                officialMessage(), recipient("openId", "openid-1"), true, resolver);

        assertEquals("DRY_RUN", result.code());
        assertEquals(0, resolutions.get());
    }

    @Test
    void mapsRecipientAndRateLimitErrors() throws Exception {
        try (MockWeChatApi api = new MockWeChatApi()) {
            api.sendResponses.add("{\"errcode\":43004,\"errmsg\":\"require subscribe\"}");
            ProviderResult recipient = send(new WeChatOfficialProviderFactory(api.base()), officialAccount(),
                    officialMessage(), recipient("openId", "openid-1"), false);
            api.sendResponses.add("{\"errcode\":45009,\"errmsg\":\"reach max api daily quota limit\"}");
            ProviderResult limited = send(new WeChatOfficialProviderFactory(api.base()), officialAccount(),
                    officialMessage(), recipient("openId", "openid-1"), false);

            assertEquals(ErrorCategory.RECIPIENT_INVALID, recipient.category());
            assertEquals(ErrorCategory.RATE_LIMITED, limited.category());
            assertTrue(limited.retryable());
        }
    }

    @Test
    void connectionTestValidatesCredentialAndCustomHostsAreRejected() throws Exception {
        try (MockWeChatApi api = new MockWeChatApi()) {
            var result = new WeComAppProviderFactory(api.base()).testConnection(
                    weComAccount(), secrets(), Duration.ofSeconds(5));
            assertTrue(result.successful());
            assertEquals("ACCESS_TOKEN_VERIFIED", result.code());
        }
        boolean rejected = false;
        try { new WeChatMiniProviderFactory(URI.create("https://attacker.example")); }
        catch (IllegalArgumentException expected) { rejected = true; }
        assertTrue(rejected);
    }

    @Test
    void treatsMalformedMessageResponseAsUnknown() throws Exception {
        try (MockWeChatApi api = new MockWeChatApi()) {
            api.sendResponses.add("not-json");
            ProviderResult result = send(new WeChatOfficialProviderFactory(api.base()), officialAccount(),
                    officialMessage(), recipient("openId", "openid-1"), false);

            assertEquals(ItemState.UNKNOWN, result.outcome());
            assertEquals("WECHAT_RESPONSE_INVALID", result.code());
        }
    }

    private static ProviderResult send(ProviderFactory factory, ConfigDocument account,
                                       ConfigDocument message, RecipientRecord recipient,
                                       boolean dryRun) throws Exception {
        return send(factory, account, message, recipient, dryRun, secrets());
    }

    private static ProviderResult send(ProviderFactory factory, ConfigDocument account,
                                       ConfigDocument message, RecipientRecord recipient,
                                       boolean dryRun, SecretResolver resolver) throws Exception {
        RunExecutionSpec spec = new RunExecutionSpec("run-wechat",
                new ProviderRef(factory.descriptor().providerId(), factory.descriptor().implementationVersion()),
                account, message, ExecutionPolicies.defaults(), Map.of(), dryRun, Instant.now());
        try (ProviderSession session = factory.open(new ProviderOpenContext(spec, resolver,
                ExecutionClock.system()))) {
            return session.send(request(spec, message, recipient, "wechat-key"), () -> false);
        }
    }

    private static ProviderSendRequest request(RunExecutionSpec spec, ConfigDocument message,
                                               RecipientRecord recipient, String key) {
        return new ProviderSendRequest(spec.runId(), recipient.itemId(), 1,
                recipient, message, key, Instant.now().plusSeconds(5));
    }

    private static ConfigDocument officialAccount() { return appAccount("official"); }

    private static ConfigDocument miniAccount() { return appAccount("mini"); }

    private static ConfigDocument appAccount(String id) {
        return json("account", """
                {"appId":"app-%s","appSecret":{"namespace":"wechat","name":"secret","version":"v1"}}
                """.formatted(id));
    }

    private static ConfigDocument weComAccount() {
        return json("account", """
                {"corpId":"corp-id","corpSecret":{"namespace":"wecom","name":"secret","version":"v1"},
                 "agentId":100001}
                """);
    }

    private static ConfigDocument officialMessage() {
        return json("message", """
                {"type":"TEMPLATE","payloadJsonTemplate":"{\\\"template_id\\\":\\\"tpl-1\\\",\\\"data\\\":{\\\"name\\\":{\\\"value\\\":\\\"{{name}}\\\"}}}"}
                """);
    }

    private static ConfigDocument miniMessage() {
        return json("message", """
                {"type":"SUBSCRIBE","payloadJsonTemplate":"{\\\"template_id\\\":\\\"tpl-mini\\\",\\\"data\\\":{\\\"thing1\\\":{\\\"value\\\":\\\"{{name}}\\\"}}}"}
                """);
    }

    private static ConfigDocument weComMessage() {
        return json("message", """
                {"type":"APP","payloadJsonTemplate":"{\\\"msgtype\\\":\\\"text\\\",\\\"text\\\":{\\\"content\\\":\\\"Hello {{name}}\\\"}}"}
                """);
    }

    private static RecipientRecord recipient(String targetName, String targetValue) {
        return new RecipientRecord("recipient-1", 0, Map.of(
                targetName, new RecipientValue.TextValue(targetValue),
                "name", new RecipientValue.TextValue("Alice")));
    }

    private static SecretResolver secrets() {
        return ref -> InMemorySecretValue.of("test-app-secret");
    }

    private static ConfigDocument json(String schema, String value) {
        return new ConfigDocument(schema, "1", value.getBytes(StandardCharsets.UTF_8));
    }

    private static final class MockWeChatApi implements AutoCloseable {
        private final HttpServer server;
        private final AtomicInteger tokenCalls = new AtomicInteger();
        private final AtomicInteger sendCalls = new AtomicInteger();
        private final List<String> sendResponses = new ArrayList<>();
        private final List<String> tokenQueries = new ArrayList<>();
        private final List<String> sendQueries = new ArrayList<>();
        private final List<String> sendPaths = new ArrayList<>();
        private final List<String> requestBodies = new ArrayList<>();

        private MockWeChatApi() throws IOException {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/cgi-bin/", this::handle);
            server.start();
        }

        URI base() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort()); }

        private synchronized void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String response;
            if (path.endsWith("/token") || path.endsWith("/gettoken")) {
                int call = tokenCalls.incrementAndGet();
                tokenQueries.add(exchange.getRequestURI().getRawQuery());
                response = "{\"access_token\":\"token-" + call + "\",\"expires_in\":7200}";
            } else {
                int call = sendCalls.getAndIncrement();
                sendPaths.add(path);
                sendQueries.add(exchange.getRequestURI().getRawQuery());
                requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                response = sendResponses.isEmpty()
                        ? "{\"errcode\":0,\"errmsg\":\"ok\"}"
                        : sendResponses.get(Math.min(call, sendResponses.size() - 1));
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        @Override
        public void close() { server.stop(0); }
    }
}
