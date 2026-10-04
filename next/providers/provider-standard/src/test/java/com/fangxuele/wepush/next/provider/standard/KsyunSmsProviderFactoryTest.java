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
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KsyunSmsProviderFactoryTest {
    @Test
    void sendsSignedSmsAndMapsKsyunIdentifiers() throws Exception {
        AtomicReference<URI> requestUri = new AtomicReference<>();
        try (MockKsyunEndpoint endpoint = new MockKsyunEndpoint(requestUri,
                "{\"RequestId\":\"req-1\",\"Sid\":\"sid-1\"}")) {
            ProviderResult result = send(endpoint.factory(), endpoint.uri(), false, secrets());

            assertEquals(ItemState.SUCCEEDED, result.outcome());
            assertEquals("req-1", result.externalRequestId());
            assertEquals("sid-1", result.metadata().get("sid"));
            Map<String, String> query = query(requestUri.get());
            assertEquals("SendSms", query.get("Action"));
            assertEquals("13800138000", query.get("Mobile"));
            assertEquals("{\"code\":\"7312\"}", query.get("TplParams"));
            assertEquals("ksms", query.get("Service"));
            assertEquals("2019-05-01", query.get("Version"));
            assertEquals("cn-beijing-6", query.get("Region"));
            assertEquals("test-id", query.get("Accesskey"));
            assertEquals("123", query.get("TplId"));
            assertEquals(expectedSignature(query, "test-secret"), query.get("Signature"));
        }
    }

    @Test
    void classifiesRejectedMobileNumber() throws Exception {
        try (MockKsyunEndpoint endpoint = new MockKsyunEndpoint(new AtomicReference<>(),
                "{\"Error\":{\"Code\":\"InvalidMobile\",\"Message\":\"invalid\"}}")) {
            ProviderResult result = send(endpoint.factory(), endpoint.uri(), false, secrets());

            assertEquals(ItemState.FAILED, result.outcome());
            assertEquals(ErrorCategory.RECIPIENT_INVALID, result.category());
            assertFalse(result.retryable());
        }
    }

    @Test
    void dryRunRendersJsonWithoutResolvingCredentialOrCallingNetwork() throws Exception {
        AtomicInteger resolutions = new AtomicInteger();
        ProviderResult result = send(new KsyunSmsProviderFactory(), URI.create("https://unused.invalid"),
                true, ref -> {
                    resolutions.incrementAndGet();
                    throw new AssertionError("Dry Run must not resolve secrets");
                });

        assertEquals("DRY_RUN", result.code());
        assertEquals(0, resolutions.get());
    }

    @Test
    void connectionTestChecksCredentialPresenceAndEndpointReachabilityOnly() throws Exception {
        try (MockKsyunEndpoint endpoint = new MockKsyunEndpoint(new AtomicReference<>(), "{}")) {
            var result = endpoint.factory().testConnection(account(), secrets(), Duration.ofSeconds(5));

            assertTrue(result.successful());
            assertEquals("KSYUN_ENDPOINT_REACHABLE", result.code());
        }
    }

    @Test
    void validatesSmsConfigurationAndRestrictsCustomEndpoints() {
        KsyunSmsProviderFactory factory = new KsyunSmsProviderFactory();
        var message = factory.validateMessage(json("message", """
                {"signName":"WePush","templateId":"bad code"}
                """));

        assertFalse(message.validResult());
        assertEquals("INVALID_TEMPLATE_CODE", message.violations().getFirst().code());
        boolean rejected = false;
        try { new KsyunSmsProviderFactory(URI.create("https://attacker.example/")); }
        catch (IllegalArgumentException expected) { rejected = true; }
        assertTrue(rejected);
    }

    @Test
    void treatsMalformedAcceptedResponseAsUnknown() throws Exception {
        try (MockKsyunEndpoint endpoint = new MockKsyunEndpoint(new AtomicReference<>(), "not-json")) {
            ProviderResult result = send(endpoint.factory(), endpoint.uri(), false, secrets());

            assertEquals(ItemState.UNKNOWN, result.outcome());
            assertEquals("KSYUN_RESPONSE_INVALID", result.code());
        }
    }

    @Test
    void matchesIndependentSignatureVector() throws Exception {
        Map<String, String> values = new TreeMap<>(Map.of(
                "Action", "SendSms", "Accesskey", "test-id", "Mobile", "13800138000",
                "Region", "cn-beijing-6", "Service", "ksms", "SignName", "测试 +*~/",
                "SignatureMethod", "HMAC-SHA256", "SignatureVersion", "1.0",
                "Timestamp", "2026-10-04T00:00:00Z", "TplId", "123"));
        values.put("TplParams", "{\"name\":\"张三 &+*~/\"}");
        values.put("Version", "2019-05-01");
        assertEquals("9cc1e48b5cc82618440a0d0e1a0d4b9987ad9d868e4d16af1900ce52d4df5fac",
                KsyunSmsProviderSession.signature(values, "test-secret"));
    }

    @Test
    void refusesEmptySuccessAndClassifiesAuthenticationAndRateLimitErrors() throws Exception {
        try (MockKsyunEndpoint endpoint = new MockKsyunEndpoint(new AtomicReference<>(), "{}")) {
            assertEquals(ItemState.UNKNOWN, send(endpoint.factory(), endpoint.uri(), false, secrets()).outcome());
        }
        try (MockKsyunEndpoint endpoint = new MockKsyunEndpoint(new AtomicReference<>(),
                "{\"Error\":{\"Code\":\"SignatureDoesNotMatch\"}}")) {
            ProviderResult result = send(endpoint.factory(), endpoint.uri(), false, secrets());
            assertEquals(ErrorCategory.AUTHENTICATION, result.category());
            assertFalse(result.retryable());
        }
        try (MockKsyunEndpoint endpoint = new MockKsyunEndpoint(new AtomicReference<>(),
                "{\"Error\":{\"Code\":\"Throttling\"}}")) {
            assertTrue(send(endpoint.factory(), endpoint.uri(), false, secrets()).retryable());
        }
    }

    @Test
    void supportsBothOfficialRegionsAndRejectsUnsupportedRegion() {
        assertTrue(new KsyunSmsProviderFactory(URI.create("https://ksms.ksyun.com/")).descriptor()
                .providerId().equals("wepush.sms.ksyun"));
        assertTrue(new KsyunSmsProviderFactory(URI.create("https://ksms.cn-guangzhou-1.api.ksyun.com/"))
                .validateAccount(account()).validResult());
        assertFalse(new KsyunSmsProviderFactory().validateAccount(json("account",
                "{\"accessKeyId\":\"test-id\",\"accessKeySecret\":{\"namespace\":\"ksyun\","
                        + "\"name\":\"secret\",\"version\":\"v1\"},\"region\":\"cn-other\"}")).validResult());
    }

    private static ProviderResult send(KsyunSmsProviderFactory factory, URI ignored,
                                       boolean dryRun, SecretResolver resolver) throws Exception {
        ConfigDocument message = json("message", """
                {"signName":"WePush","templateId":"123",
                 "templateParamJsonTemplate":"{\\\"code\\\":\\\"{{code}}\\\"}"}
                """);
        RunExecutionSpec spec = new RunExecutionSpec("run-sms",
                new ProviderRef(KsyunSmsProviderFactory.PROVIDER_ID, KsyunSmsProviderFactory.VERSION),
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
                 "accessKeySecret":{"namespace":"ksyun","name":"secret","version":"v1"}}
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
        String canonical = unsigned.entrySet().stream().map(entry ->
                java.net.URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8).replace("+", "%20")
                + "=" + java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)
                .replace("+", "%20").replace("*", "%2A").replace("%7E", "~"))
                .collect(java.util.stream.Collectors.joining("&"));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return java.util.HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    }

    private static ConfigDocument json(String schema, String value) {
        return new ConfigDocument(schema, "1", value.getBytes(StandardCharsets.UTF_8));
    }

    private static final class MockKsyunEndpoint implements AutoCloseable {
        private final HttpServer server;
        private final URI uri;

        private MockKsyunEndpoint(AtomicReference<URI> requestUri, String response) throws Exception {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/", exchange -> {
                if (!exchange.getRequestMethod().equals("HEAD")) {
                    assertEquals("POST", exchange.getRequestMethod());
                    assertTrue(exchange.getRequestHeaders().getFirst("Content-Type")
                            .startsWith("application/x-www-form-urlencoded"));
                    String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    requestUri.set(URI.create("http://localhost/?" + form));
                }
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

        KsyunSmsProviderFactory factory() { return new KsyunSmsProviderFactory(uri); }

        @Override
        public void close() { server.stop(0); }
    }
}
