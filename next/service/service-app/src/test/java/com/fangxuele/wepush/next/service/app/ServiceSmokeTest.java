package com.fangxuele.wepush.next.service.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "wepush.agent.grpc.port=0")
class ServiceSmokeTest {
    private static final Path DATABASE = Path.of(System.getProperty("java.io.tmpdir"),
            "wepush-next-smoke-" + UUID.randomUUID() + ".db");
    private static final Path MASTER_KEY = Path.of(System.getProperty("java.io.tmpdir"),
            "wepush-next-smoke-key-" + UUID.randomUUID() + ".json");
    private static final Path ARTIFACT_ROOT = Path.of(System.getProperty("java.io.tmpdir"),
            "wepush-next-smoke-artifacts-" + UUID.randomUUID());

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void isolation(DynamicPropertyRegistry registry) {
        registry.add("wepush.database.path", DATABASE::toString);
        registry.add("wepush.secret.master-key-path", MASTER_KEY::toString);
        registry.add("wepush.artifact.root", ARTIFACT_ROOT::toString);
        registry.add("server.shutdown", () -> "immediate");
    }

    @Test
    void exposesHealthSystemInfoProviderCatalogAndSchemas() throws Exception {
        HttpResponse<String> health = get("/actuator/health");
        HttpResponse<String> info = get("/api/v1/system/info");
        HttpResponse<String> providers = get("/api/v1/providers");
        HttpResponse<String> schema = get(
                "/api/v1/providers/wepush.http/versions/0.1.0/schemas/account");
        HttpResponse<String> openApi = get("/openapi.yaml");
        HttpResponse<String> desktopPreflight = desktopPreflight("/api/v1/system/info");

        assertEquals(200, health.statusCode());
        assertTrue(health.body().contains("UP"));
        assertEquals(200, info.statusCode());
        assertTrue(info.body().contains("WePush Next"));
        assertEquals(200, providers.statusCode());
        assertTrue(providers.body().contains("wepush.http"));
        assertEquals(200, schema.statusCode());
        assertTrue(schema.body().contains("HTTP Account"));
        assertEquals(200, openApi.statusCode());
        assertTrue(openApi.body().contains("openapi: 3.1.0"));
        assertEquals(200, desktopPreflight.statusCode());
        assertEquals("wepush://app", desktopPreflight.headers()
                .firstValue("Access-Control-Allow-Origin").orElseThrow());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> desktopPreflight(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + path))
                .header("Origin", "wepush://app")
                .header("Access-Control-Request-Method", "GET")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
