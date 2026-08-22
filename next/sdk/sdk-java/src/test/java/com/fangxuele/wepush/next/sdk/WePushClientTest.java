package com.fangxuele.wepush.next.sdk;

import com.fangxuele.wepush.next.service.api.ControlPlaneApi;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
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

    @Test
    void createsRunThroughRemoteApiWithoutDependingOnCore() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> idempotencyKey = new AtomicReference<>();
        AtomicReference<String> requestMethod = new AtomicReference<>();
        server.createContext("/api/v1/workspaces/ws_default/jobs/job_1/runs", exchange -> {
            idempotencyKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            requestMethod.set(exchange.getRequestMethod());
            byte[] body = """
                    {
                      "id":"run_1","workspaceId":"ws_default","jobId":"job_1","state":"PENDING",
                      "stateReason":"manual","dryRun":true,
                      "counters":{"total":1,"succeeded":0,"failed":0,"unknown":0,"unsent":0,"skipped":0,"retried":0},
                      "createdAt":"2026-08-22T10:00:00Z","startedAt":null,"endedAt":null,
                      "updatedAt":"2026-08-22T10:00:00Z","version":0,"links":{}
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(202, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try (WePushClient client = WePushClient.builder()
                .endpoint(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
                .build()) {
            ControlPlaneApi.RunResponse run = client.workspace("ws_default").createRun(
                    "job_1", "sdk-run-1", new ControlPlaneApi.CreateRunRequest(true, Map.of(), "manual"));
            assertEquals("run_1", run.id());
        }

        assertEquals("POST", requestMethod.get());
        assertEquals("sdk-run-1", idempotencyKey.get());
    }

    @Test
    void pagesPersistedRunItemResults() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> query = new AtomicReference<>();
        server.createContext("/api/v1/workspaces/ws_default/runs/run_1/items", exchange -> {
            query.set(exchange.getRequestURI().getQuery());
            byte[] body = """
                    {"items":[{"runId":"run_1","itemId":"alice","attempts":1,"state":"SUCCEEDED",
                    "providerCode":"DRY_RUN","diagnostic":"","externalRequestId":"",
                    "completedAt":"2026-08-22T10:00:00Z","metadata":{}}],
                    "page":{"nextCursor":"next.abc","hasMore":true}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try (WePushClient client = WePushClient.builder()
                .endpoint(URI.create("http://127.0.0.1:" + server.getAddress().getPort())).build()) {
            ControlPlaneApi.RunItemResultPage page = client.workspace("ws_default")
                    .runItems("run_1", "cursor.abc", 1);
            assertEquals("alice", page.items().getFirst().itemId());
            assertEquals("next.abc", page.page().nextCursor());
        }
        assertEquals("limit=1&cursor=cursor.abc", query.get());
    }

    @Test
    void createsListsAndDownloadsRunArtifacts() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String artifactJson = """
                {"id":"artifact_1","workspaceId":"ws_default","runId":"run_1",
                "type":"RUN_RESULTS_CSV","backend":"LOCAL_FILE","originalName":"run-results.csv",
                "contentType":"text/csv; charset=utf-8","size":7,"sha256":"abc","state":"READY",
                "expiresAt":"2026-08-23T10:00:00Z","pinned":false,"legalHold":false,
                "createdAt":"2026-08-22T10:00:00Z","readyAt":"2026-08-22T10:00:01Z",
                "deletedAt":null,"version":1,"links":{}}
                """;
        server.createContext("/api/v1/workspaces/ws_default/runs/run_1/artifacts/result-export", exchange -> {
            byte[] body = artifactJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/api/v1/workspaces/ws_default/runs/run_1/artifacts", exchange -> {
            byte[] body = ("[" + artifactJson + "]").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/api/v1/workspaces/ws_default/artifacts/artifact_1/content", exchange -> {
            byte[] body = "item_id".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/csv; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try (WePushClient client = WePushClient.builder()
                .endpoint(URI.create("http://127.0.0.1:" + server.getAddress().getPort())).build()) {
            WorkspaceClient workspace = client.workspace("ws_default");
            assertEquals("artifact_1", workspace.createResultExport("run_1").id());
            assertEquals("RUN_RESULTS_CSV", workspace.runArtifacts("run_1").getFirst().type());
            try (InputStream content = workspace.downloadArtifact("artifact_1")) {
                assertEquals("item_id", new String(content.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }
}
