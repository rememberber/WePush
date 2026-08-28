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
import com.fangxuele.wepush.next.core.api.SecretResolver;
import com.fangxuele.wepush.next.provider.spi.ErrorCategory;
import com.fangxuele.wepush.next.provider.spi.ProviderOpenContext;
import com.fangxuele.wepush.next.provider.spi.ProviderResult;
import com.fangxuele.wepush.next.provider.spi.ProviderSendRequest;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AliyunSmsProviderFactoryTest {
    @Test
    void sendsSignedSmsAndMapsAliyunIdentifiers() throws Exception {
        AtomicReference<URI> requestUri = new AtomicReference<>();
        try (MockAliyunEndpoint endpoint = new MockAliyunEndpoint(requestUri,
                "{\"Code\":\"OK\",\"RequestId\":\"req-1\",\"BizId\":\"biz-1\"}")) {
            ProviderResult result = send(endpoint.factory(), endpoint.uri(), false, secrets());

            assertEquals(ItemState.SUCCEEDED, result.outcome());
            assertEquals("req-1", result.externalRequestId());
            assertEquals("biz-1", result.metadata().get("bizId"));
            Map<String, String> query = query(requestUri.get());
            assertEquals("SendSms", query.get("Action"));
            assertEquals("13800138000", query.get("PhoneNumbers"));
            assertEquals("{\"code\":\"7312\"}", query.get("TemplateParam"));
            assertEquals("sms-key", query.get("OutId"));
            assertEquals(expectedSignature(query, "test-secret"), query.get("Signature"));
        }
    }

    @Test
    void classifiesRejectedMobileNumber() throws Exception {
        try (MockAliyunEndpoint endpoint = new MockAliyunEndpoint(new AtomicReference<>(),
                "{\"Code\":\"isv.MOBILE_NUMBER_ILLEGAL\",\"Message\":\"invalid\"}")) {
            ProviderResult result = send(endpoint.factory(), endpoint.uri(), false, secrets());

            assertEquals(ItemState.FAILED, result.outcome());
            assertEquals(ErrorCategory.RECIPIENT_INVALID, result.category());
            assertFalse(result.retryable());
        }
    }

    @Test
    void dryRunRendersJsonWithoutResolvingCredentialOrCallingNetwork() throws Exception {
        AtomicInteger resolutions = new AtomicInteger();
        ProviderResult result = send(new AliyunSmsProviderFactory(), URI.create("https://unused.invalid"),
                true, ref -> {
                    resolutions.incrementAndGet();
                    throw new AssertionError("Dry Run must not resolve secrets");
                });

        assertEquals("DRY_RUN", result.code());
        assertEquals(0, resolutions.get());
    }

    @Test
    void connectionTestChecksCredentialPresenceAndEndpointReachabilityOnly() throws Exception {
        try (MockAliyunEndpoint endpoint = new MockAliyunEndpoint(new AtomicReference<>(), "{}")) {
            var result = endpoint.factory().testConnection(account(), secrets(), Duration.ofSeconds(5));

            assertTrue(result.successful());
            assertEquals("ALIYUN_ENDPOINT_REACHABLE", result.code());
        }
    }

    @Test
    void validatesSmsConfigurationAndRestrictsCustomEndpoints() {
        AliyunSmsProviderFactory factory = new AliyunSmsProviderFactory();
        var message = factory.validateMessage(json("message", """
                {"signName":"WePush","templateCode":"bad code"}
                """));

        assertFalse(message.validResult());
        assertEquals("INVALID_TEMPLATE_CODE", message.violations().getFirst().code());
        boolean rejected = false;
        try { new AliyunSmsProviderFactory(URI.create("https://attacker.example/")); }
        catch (IllegalArgumentException expected) { rejected = true; }
        assertTrue(rejected);
    }

    @Test
    void treatsMalformedAcceptedResponseAsUnknown() throws Exception {
        try (MockAliyunEndpoint endpoint = new MockAliyunEndpoint(new AtomicReference<>(), "not-json")) {
            ProviderResult result = send(endpoint.factory(), endpoint.uri(), false, secrets());

            assertEquals(ItemState.UNKNOWN, result.outcome());
            assertEquals("ALIYUN_RESPONSE_INVALID", result.code());
        }
    }

    private static ProviderResult send(AliyunSmsProviderFactory factory, URI ignored,
                                       boolean dryRun, SecretResolver resolver) throws Exception {
        ConfigDocument message = json("message", """
                {"signName":"WePush","templateCode":"SMS_123",
                 "templateParamJsonTemplate":"{\\\"code\\\":\\\"{{code}}\\\"}"}
                """);
        RunExecutionSpec spec = new RunExecutionSpec("run-sms",
                new ProviderRef(AliyunSmsProviderFactory.PROVIDER_ID, AliyunSmsProviderFactory.VERSION),
                account(), message, ExecutionPolicies.defaults(), Map.of(), dryRun, Instant.now());
        try (ProviderSession session = factory.open(new ProviderOpenContext(spec, resolver,
                ExecutionClock.system()))) {
            return session.send(new ProviderSendRequest(spec.runId(), "sms-1", 1,
                    new RecipientRecord("sms-1", 0, Map.of(
                            "phoneNumber", new RecipientValue.TextValue("13800138000"),
                            "code", new RecipientValue.TextValue("7312"))),
                    message, "sms-key", Instant.now().plusSeconds(5)), () -> false);
        }
    }

    private static ConfigDocument account() {
        return json("account", """
                {"accessKeyId":"test-id",
                 "accessKeySecret":{"namespace":"aliyun","name":"secret","version":"v1"}}
                """);
    }

    private static SecretResolver secrets() {
        return ref -> InMemorySecretValue.of("test-secret");
    }

    private static Map<String, String> query(URI uri) {
        Map<String, String> values = new TreeMap<>();
        for (String pair : uri.getRawQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts.length == 1 ? "" : parts[1], StandardCharsets.UTF_8));
        }
        return values;
    }

    private static String expectedSignature(Map<String, String> query, String secret) throws Exception {
        TreeMap<String, String> unsigned = new TreeMap<>(query);
        unsigned.remove("Signature");
        StringBuilder canonical = new StringBuilder();
        unsigned.forEach((key, value) -> canonical.append('&')
                .append(AliyunSmsProviderSession.percentEncode(key)).append('=')
                .append(AliyunSmsProviderSession.percentEncode(value)));
        String stringToSign = "GET&%2F&" + AliyunSmsProviderSession.percentEncode(canonical.substring(1));
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((secret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
    }

    private static ConfigDocument json(String schema, String value) {
        return new ConfigDocument(schema, "1", value.getBytes(StandardCharsets.UTF_8));
    }

    private static final class MockAliyunEndpoint implements AutoCloseable {
        private final HttpServer server;
        private final URI uri;

        private MockAliyunEndpoint(AtomicReference<URI> requestUri, String response) throws Exception {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/", exchange -> {
                requestUri.set(exchange.getRequestURI());
                byte[] body = response.getBytes(StandardCharsets.UTF_8);
                if (exchange.getRequestMethod().equals("HEAD")) {
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
                exchange.close();
            });
            server.start();
            uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
        }

        URI uri() { return uri; }

        AliyunSmsProviderFactory factory() { return new AliyunSmsProviderFactory(uri); }

        @Override
        public void close() { server.stop(0); }
    }
}
