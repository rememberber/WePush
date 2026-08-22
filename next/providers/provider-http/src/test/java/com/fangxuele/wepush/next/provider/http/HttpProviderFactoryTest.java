package com.fangxuele.wepush.next.provider.http;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RecipientValue;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.provider.spi.ErrorCategory;
import com.fangxuele.wepush.next.provider.spi.ProviderOpenContext;
import com.fangxuele.wepush.next.provider.spi.ProviderResult;
import com.fangxuele.wepush.next.provider.spi.ProviderSendRequest;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpProviderFactoryTest {
    private final HttpProviderFactory factory = new HttpProviderFactory();

    @Test
    void sendsTemplatedRequestWithIdempotencyAndClassifiesSuccess() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> idempotencyKey = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/notify", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            idempotencyKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            exchange.getResponseHeaders().add("X-Request-Id", "remote-123");
            exchange.sendResponseHeaders(201, 0);
            exchange.close();
        });
        server.start();

        try {
            ConfigDocument account = json("account", """
                    {
                      "baseUrl": "http://127.0.0.1:%d",
                      "allowPrivateAddresses": true,
                      "connectTimeout": "PT2S"
                    }
                    """.formatted(server.getAddress().getPort()));
            ConfigDocument message = json("message", """
                    {
                      "method": "POST",
                      "path": "/notify",
                      "headers": {"Content-Type": "application/json"},
                      "bodyTemplate": "{\\\"name\\\":\\\"{{name}}\\\"}",
                      "successStatuses": [201],
                      "idempotencyHeader": "Idempotency-Key"
                    }
                    """);
            RecipientRecord recipient = new RecipientRecord(
                    "item-1", 0, Map.of("name", new RecipientValue.TextValue("A\"lice")));

            ProviderResult result = send(account, message, recipient, false);

            assertEquals(com.fangxuele.wepush.next.core.api.ItemState.SUCCEEDED, result.outcome());
            assertEquals("HTTP_201", result.code());
            assertEquals("remote-123", result.externalRequestId());
            assertEquals("run-1:item-1", idempotencyKey.get());
            assertEquals("{\"name\":\"A\\\"lice\"}", requestBody.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void blocksLoopbackTargetsByDefault() throws Exception {
        ConfigDocument account = json("account", """
                {"baseUrl":"http://127.0.0.1:9"}
                """);
        ConfigDocument message = json("message", """
                {"method":"POST","path":"/notify","bodyTemplate":"{}"}
                """);

        ProviderResult result = send(account, message, recipient(), false);

        assertEquals("SSRF_BLOCKED", result.code());
        assertEquals(ErrorCategory.INVALID_REQUEST, result.category());
        assertFalse(result.retryable());
    }

    @Test
    void dryRunDoesNotOpenNetworkConnection() throws Exception {
        ConfigDocument account = json("account", """
                {"baseUrl":"http://127.0.0.1:9"}
                """);
        ConfigDocument message = json("message", """
                {"method":"POST","path":"/notify","bodyTemplate":"{}"}
                """);

        ProviderResult result = send(account, message, recipient(), true);

        assertEquals("DRY_RUN", result.code());
        assertEquals(com.fangxuele.wepush.next.core.api.ItemState.SUCCEEDED, result.outcome());
    }

    @Test
    void reportsStructuredValidationErrors() {
        ConfigDocument account = json("account", """
                {"baseUrl":"file:///tmp/not-allowed"}
                """);
        ConfigDocument message = json("message", """
                {"method":"TRACE","path":"https://evil.example/"}
                """);

        var accountValidation = factory.validateAccount(account);
        var messageValidation = factory.validateMessage(message);

        assertFalse(accountValidation.validResult());
        assertEquals("INVALID_BASE_URL", accountValidation.violations().getFirst().code());
        assertFalse(messageValidation.validResult());
        assertEquals("UNSUPPORTED_METHOD", messageValidation.violations().getFirst().code());
    }

    private ProviderResult send(
            ConfigDocument account,
            ConfigDocument message,
            RecipientRecord recipient,
            boolean dryRun
    ) throws Exception {
        RunExecutionSpec spec = new RunExecutionSpec(
                "run-1",
                new ProviderRef(HttpProviderFactory.PROVIDER_ID, HttpProviderFactory.VERSION),
                account,
                message,
                ExecutionPolicies.defaults(),
                Map.of(),
                dryRun,
                Instant.now());
        try (ProviderSession session = factory.open(new ProviderOpenContext(
                spec,
                ref -> {
                    throw new AssertionError("No secret expected");
                },
                ExecutionClock.system()))) {
            return session.send(new ProviderSendRequest(
                    spec.runId(), recipient.itemId(), 1, recipient, message,
                    spec.runId() + ":" + recipient.itemId(), Instant.now().plusSeconds(5)),
                    () -> false);
        }
    }

    private static RecipientRecord recipient() {
        return new RecipientRecord(
                "item-1", 0, Map.of("name", new RecipientValue.TextValue("Alice")));
    }

    private static ConfigDocument json(String schema, String value) {
        return new ConfigDocument(schema, "1", value.getBytes(StandardCharsets.UTF_8));
    }
}
