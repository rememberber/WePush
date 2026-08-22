package com.fangxuele.wepush.next.sdk;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WePushClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void callsPublicApiWithBearerTokenAndRetriesTransientStatus() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/api/v1/system/info", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            int attempt = attempts.incrementAndGet();
            if (attempt == 1) {
                exchange.getResponseHeaders().add("Retry-After", "0");
                exchange.sendResponseHeaders(503, -1);
            } else {
                byte[] body = """
                        {"product":"WePush Next","version":"0.1.0","mode":"standalone","serverTime":"2026-08-22T10:00:00Z"}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();

        try (WePushClient client = WePushClient.builder()
                .endpoint(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
                .token(() -> "test-token")
                .retryPolicy(new RetryPolicy(2, Duration.ZERO, Duration.ZERO))
                .build()) {
            assertEquals("WePush Next", client.system().info().product());
        }

        assertEquals(2, attempts.get());
        assertEquals("Bearer test-token", authorization.get());
    }

    @Test
    void rejectsEndpointsThatContainCredentials() {
        WePushClient.Builder builder = WePushClient.builder()
                .endpoint(URI.create("https://user:password@example.com"));

        assertThrows(IllegalStateException.class, builder::build);
    }
}
