package com.fangxuele.wepush.next.service.app;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "wepush.soak.seconds", matches = "[1-9][0-9]*")
class StandaloneSoakIntegrationTest {
    private static final Path SQLITE_PATH = Path.of(System.getProperty("java.io.tmpdir"),
            "wepush-next-soak-" + UUID.randomUUID() + ".db");

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String database = System.getenv("WEPUSH_SOAK_DATABASE");
        if ("postgres".equalsIgnoreCase(database) || "postgresql".equalsIgnoreCase(database)) {
            registry.add("wepush.database.kind", () -> "postgresql");
            registry.add("wepush.database.url", () -> System.getenv().getOrDefault(
                    "WEPUSH_TEST_POSTGRES_URL", "jdbc:postgresql://127.0.0.1:5432/wepush"));
            registry.add("wepush.database.username", () -> System.getenv().getOrDefault(
                    "WEPUSH_TEST_POSTGRES_USERNAME", "wepush"));
            registry.add("wepush.database.password", () -> System.getenv().getOrDefault(
                    "WEPUSH_TEST_POSTGRES_PASSWORD", "wepush-test"));
        } else {
            registry.add("wepush.database.kind", () -> "sqlite");
            registry.add("wepush.database.path", SQLITE_PATH::toString);
        }
        registry.add("wepush.grpc.port", () -> "0");
        registry.add("wepush.schedule.enabled", () -> "false");
    }

    @Test
    void installationHealthRemainsStableForConfiguredDuration() throws Exception {
        String configured = System.getProperty("wepush.soak.seconds");
        if (configured == null || configured.isBlank()) throw new IllegalStateException("Missing soak duration");
        long seconds = Math.max(5, Math.min(3_600, Long.parseLong(configured)));
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        Instant deadline = Instant.now().plusSeconds(seconds);
        int checks = 0;
        while (Instant.now().isBefore(deadline)) {
            assertJsonContains(client, "/actuator/health/installation", "\"status\":\"UP\"", "\"dryRun\":\"DRY_RUN\"");
            assertJsonContains(client, "/api/v1/system/info", "\"product\":\"WePush Next\"");
            checks++;
            Thread.sleep(100);
        }
        assertThat(checks).isGreaterThanOrEqualTo(10);
    }

    private void assertJsonContains(HttpClient client, String path, String... fragments) throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(fragments);
    }

    @AfterAll
    static void cleanup() throws Exception {
        Files.deleteIfExists(SQLITE_PATH);
        Files.deleteIfExists(Path.of(SQLITE_PATH + "-shm"));
        Files.deleteIfExists(Path.of(SQLITE_PATH + "-wal"));
    }
}
