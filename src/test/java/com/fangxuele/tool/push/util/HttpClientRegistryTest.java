package com.fangxuele.tool.push.util;

import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import org.junit.After;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HttpClientRegistryTest {
    private static final int MESSAGE_TYPE = 90_001;
    private static final int ACCOUNT_ID = 90_002;

    @After
    public void tearDown() {
        System.clearProperty(HttpClientRegistry.PROTOCOL_PROPERTY);
        HttpClientRegistry.invalidate(MESSAGE_TYPE, ACCOUNT_ID);
    }

    @Test
    public void shouldReuseClientAndAllowHttp1ControlGroup() {
        OkHttpClient first = HttpClientRegistry.get(MESSAGE_TYPE, ACCOUNT_ID);
        OkHttpClient second = HttpClientRegistry.get(MESSAGE_TYPE, ACCOUNT_ID);
        assertSame(first, second);
        assertTrue(first.protocols().contains(Protocol.HTTP_2));

        System.setProperty(HttpClientRegistry.PROTOCOL_PROPERTY, "http1");
        OkHttpClient http1 = HttpClientRegistry.get(MESSAGE_TYPE, ACCOUNT_ID);
        assertNotSame(first, http1);
        assertEquals(List.of(Protocol.HTTP_1_1), http1.protocols());
    }

    @Test
    public void shouldCloseResponsesReuseConnectionAndCaptureMetrics() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> {
            byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/limited", exchange -> {
            exchange.getResponseHeaders().add("Retry-After", "3");
            byte[] response = "limited".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(429, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            OkHttpClient client = HttpClientRegistry.get(MESSAGE_TYPE, ACCOUNT_ID);
            HttpClientRegistry.MetricsSnapshot before = HttpClientRegistry.snapshot(MESSAGE_TYPE, ACCOUNT_ID);
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

            for (int i = 0; i < 2; i++) {
                OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(client,
                        new Request.Builder().url(baseUrl + "/ok").build());
                assertEquals(200, response.statusCode());
                assertEquals("ok", response.body());
            }
            OkHttpRequestUtil.ResponseData limited = OkHttpRequestUtil.execute(client,
                    new Request.Builder().url(baseUrl + "/limited").build());
            assertEquals(Long.valueOf(3_000), limited.retryAfterMillis());

            HttpClientRegistry.MetricsSnapshot delta = HttpClientRegistry.snapshot(MESSAGE_TYPE, ACCOUNT_ID).minus(before);
            assertEquals(3, delta.calls());
            assertEquals(3, delta.http1Calls());
            assertEquals(1, delta.connections());
            assertTrue(delta.reusedConnections() >= 2);
            assertEquals(1, delta.throttledResponses());
        } finally {
            server.stop(0);
        }
    }
}
