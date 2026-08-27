package com.fangxuele.wepush.next.service.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ControlPlaneApiTest {
    private static final Path DATABASE = Path.of(System.getProperty("java.io.tmpdir"),
            "wepush-next-control-plane-" + UUID.randomUUID() + ".db");
    private static final Path MASTER_KEY = Path.of(System.getProperty("java.io.tmpdir"),
            "wepush-next-control-plane-key-" + UUID.randomUUID() + ".json");
    private static final Path ARTIFACT_ROOT = Path.of(System.getProperty("java.io.tmpdir"),
            "wepush-next-artifacts-" + UUID.randomUUID());
    private static final AtomicBoolean FAIL_BOB = new AtomicBoolean(true);
    private static final AtomicInteger PROVIDER_SENDS = new AtomicInteger();
    private static HttpServer providerServer;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private JdbcTemplate jdbc;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void startProvider() throws Exception {
        providerServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        providerServer.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            boolean fails = !exchange.getRequestMethod().equals("HEAD") && FAIL_BOB.get()
                    && PROVIDER_SENDS.incrementAndGet() == 2;
            exchange.sendResponseHeaders(fails ? 500 : 204, -1);
            exchange.close();
        });
        providerServer.start();
    }

    @AfterAll
    static void stopProvider() {
        if (providerServer != null) providerServer.stop(0);
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("wepush.database.path", DATABASE::toString);
        registry.add("wepush.secret.master-key-path", MASTER_KEY::toString);
        registry.add("wepush.artifact.root", ARTIFACT_ROOT::toString);
        registry.add("wepush.artifact.retention-initial-delay", () -> "PT1H");
        registry.add("wepush.agent.grpc.port", () -> "0");
        registry.add("server.shutdown", () -> "immediate");
    }

    @Test
    void createsCompleteControlPlaneGraphReplaysRunAndStreamsPersistedEvent() throws Exception {
        String secretPath = "/api/v1/workspaces/ws_default/secrets/http/authorization/versions/v1";
        HttpResponse<String> secretWrite = put(secretPath, "{\"value\":\"Bearer must-not-leak\"}");
        assertEquals(200, secretWrite.statusCode(), secretWrite.body());
        assertTrue(secretWrite.body().contains("\"configured\":true"));
        assertTrue(!secretWrite.body().contains("must-not-leak"));
        HttpResponse<String> secretMetadata = get(secretPath);
        assertEquals(200, secretMetadata.statusCode(), secretMetadata.body());
        assertTrue(!secretMetadata.body().contains("must-not-leak"));

        JsonNode account = accepted(post("/api/v1/workspaces/ws_default/accounts", """
                {
                  "name":"Local HTTP",
                  "providerId":"wepush.http",
                  "providerVersion":"0.1.0",
                  "configuration":{"baseUrl":"http://127.0.0.1:%d","allowPrivateAddresses":true,
                                   "auth":{"type":"NONE"}}
                }
                """.formatted(providerServer.getAddress().getPort()), null), 201);
        String accountPath = "/api/v1/workspaces/ws_default/accounts/" + account.get("id").textValue();
        JsonNode connection = accepted(post(accountPath + "/connection-test",
                "{\"timeout\":\"PT2S\"}", null), 200);
        assertTrue(connection.get("successful").booleanValue(), connection.toString());
        JsonNode message = accepted(post("/api/v1/workspaces/ws_default/messages", """
                {
                  "name":"Welcome",
                  "providerId":"wepush.http",
                  "providerVersion":"0.1.0",
                  "content":{"method":"POST","path":"/notify","bodyTemplate":"{\\"to\\":\\"{{mobile}}\\"}"}
                }
                """, null), 201);
        JsonNode audience = accepted(post("/api/v1/workspaces/ws_default/audiences", """
                {
                  "name":"Sample audience",
                  "recipients":[
                    {"itemId":"alice","fields":{"mobile":"13000000000"}},
                    {"itemId":"bob","fields":{"mobile":"13100000000"}}
                  ]
                }
                """, null), 201);
        JsonNode job = accepted(post("/api/v1/workspaces/ws_default/jobs", """
                {
                  "name":"Welcome job",
                  "accountId":"%s",
                  "messageId":"%s",
                  "audienceId":"%s",
                  "policies":{
                    "concurrency":{"minimum":1,"target":1,"maximum":4},
                    "rateLimit":{"permits":1,"period":"PT1S"},
                    "retry":{"maxAttempts":1}
                  },
                  "enabled":true
                }
                """.formatted(account.get("id").textValue(), message.get("id").textValue(),
                audience.get("id").textValue()), null), 201);

        String schedulesPath = "/api/v1/workspaces/ws_default/schedules";
        JsonNode schedule = accepted(post(schedulesPath, """
                {"name":"Nightly","jobId":"%s","cronExpression":"0 0 0 1 1 *",
                 "timezone":"UTC","misfirePolicy":"SKIP","enabled":false}
                """.formatted(job.get("id").textValue()), null), 201);
        JsonNode editedSchedule = accepted(patch(schedulesPath + "/" + schedule.get("id").textValue(), """
                {"name":"Annual check","cronExpression":"0 30 0 1 1 *","timezone":"Asia/Shanghai",
                 "misfirePolicy":"FIRE_ONCE","enabled":false}
                """), 200);
        assertEquals("Annual check", editedSchedule.get("name").textValue());
        assertEquals("Asia/Shanghai", editedSchedule.get("timezone").textValue());
        assertTrue(accepted(get(schedulesPath + "?limit=1"), 200).get("items").isArray());

        String runPath = "/api/v1/workspaces/ws_default/jobs/" + job.get("id").textValue() + "/runs";
        String request = "{\"dryRun\":true,\"reason\":\"integration-test\"}";
        JsonNode run = accepted(post(runPath, request, "control-plane-test"), 202);
        JsonNode replay = accepted(post(runPath, request, "control-plane-test"), 200);

        assertEquals(run.get("id").textValue(), replay.get("id").textValue());
        assertEquals("PENDING", run.get("state").textValue());
        assertEquals(2, run.at("/counters/total").intValue());

        awaitRunState(run.get("id").textValue(), "RUNNING");
        String commandBase = "/api/v1/workspaces/ws_default/runs/"
                + run.get("id").textValue() + "/commands";
        JsonNode paused = accepted(post(commandBase + "/pause", "{}", "pause-test"), 202);
        assertEquals("ACCEPTED", paused.get("status").textValue());
        assertEquals("RUN_PAUSED", paused.get("code").textValue());
        awaitRunState(run.get("id").textValue(), "PAUSED");
        JsonNode pauseReplay = accepted(post(commandBase + "/pause", "{}", "pause-test"), 200);
        assertTrue(pauseReplay.get("replayed").booleanValue());
        JsonNode concurrency = accepted(post(commandBase + "/concurrency",
                "{\"target\":2}", "concurrency-test"), 202);
        assertEquals("CONCURRENCY_CHANGED", concurrency.get("code").textValue());
        JsonNode resumed = accepted(post(commandBase + "/resume", "{}", "resume-test"), 202);
        assertEquals("RUN_RESUMED", resumed.get("code").textValue());

        JsonNode completed = awaitTerminalRun(run.get("id").textValue());
        assertEquals("SUCCEEDED", completed.get("state").textValue());
        assertEquals(2, completed.at("/counters/succeeded").intValue());

        String messagePath = "/api/v1/workspaces/ws_default/messages/" + message.get("id").textValue();
        JsonNode revisedMessage = accepted(patch(messagePath, """
                {"name":"Welcome v2","content":{"method":"POST","path":"/notify",
                 "bodyTemplate":"{\\"recipient\\":\\"{{mobile}}\\"}"}}
                """), 200);
        assertEquals(2, revisedMessage.get("revision").intValue());
        JsonNode revisions = accepted(get(messagePath + "/revisions?limit=10"), 200);
        assertEquals(2, revisions.get("items").size());
        JsonNode diff = accepted(get(messagePath + "/diff?from=1&to=2"), 200);
        assertTrue(diff.get("changedPaths").toString().contains("bodyTemplate"));
        accepted(post(messagePath + "/copy", "{\"name\":\"Welcome copy\"}", null),
                201);
        JsonNode firstMessagePage = accepted(get(
                "/api/v1/workspaces/ws_default/messages?limit=1&name=Welcome"), 200);
        assertEquals(1, firstMessagePage.get("items").size());
        assertTrue(firstMessagePage.at("/page/hasMore").booleanValue());

        String importPath = "/api/v1/workspaces/ws_default/audience-imports";
        JsonNode preview = accepted(multipart(importPath, Map.of(
                "name", "Imported audience", "audienceId", audience.get("id").textValue(),
                "format", "CSV", "itemIdColumn", "itemId",
                "fieldMapping", "{\"mobile\":\"mobile\"}"),
                "recipients.csv", "itemId,mobile\nalice,13900000000\nalice,duplicate\n,blank\nbob,13100000000\n"), 201);
        assertEquals(4, preview.get("totalRows").longValue());
        assertEquals(2, preview.get("acceptedRows").longValue());
        assertEquals(2, preview.get("rejectedRows").longValue());
        assertEquals(1, preview.get("duplicateRows").longValue());
        HttpResponse<String> importErrors = get(preview.get("errorsUrl").textValue());
        assertEquals(200, importErrors.statusCode(), importErrors.body());
        assertTrue(importErrors.body().contains("DUPLICATE_ITEM_ID"));
        JsonNode importedAudience = accepted(post(importPath + "/" + preview.get("id").textValue()
                + "/commit", "{}", null), 200);
        assertEquals(2, importedAudience.get("revision").intValue());
        assertEquals(2, importedAudience.get("recordCount").longValue());

        String frozenMessage = jdbc.queryForObject(
                "SELECT message_content_json FROM run_snapshot WHERE run_id = ?", String.class,
                run.get("id").textValue());
        String frozenAudience = jdbc.queryForObject(
                "SELECT audience_snapshot_id FROM run_snapshot WHERE run_id = ?", String.class,
                run.get("id").textValue());
        assertTrue(frozenMessage.contains("\\\"to\\\""), frozenMessage);
        assertEquals(audience.get("snapshotId").textValue(), frozenAudience);

        String itemPath = "/api/v1/workspaces/ws_default/runs/" + run.get("id").textValue() + "/items";
        JsonNode firstResultPage = accepted(get(itemPath + "?limit=1"), 200);
        assertEquals(1, firstResultPage.get("items").size());
        assertEquals("SUCCEEDED", firstResultPage.at("/items/0/state").textValue());
        assertTrue(firstResultPage.at("/page/hasMore").booleanValue());
        String cursor = firstResultPage.at("/page/nextCursor").textValue();
        JsonNode secondResultPage = accepted(get(itemPath + "?limit=1&cursor=" + cursor), 200);
        assertEquals(1, secondResultPage.get("items").size());
        assertTrue(!secondResultPage.at("/page/hasMore").booleanValue());
        String tamperedCursor = cursor.substring(0, cursor.length() - 1)
                + (cursor.endsWith("A") ? "B" : "A");
        assertEquals(400, get(itemPath + "?limit=1&cursor=" + tamperedCursor).statusCode());

        JsonNode lateCancel = accepted(post(commandBase + "/cancel",
                "{\"reason\":\"too late\"}", "cancel-test"), 409);
        assertEquals("REJECTED", lateCancel.get("status").textValue());
        assertEquals("RUN_NOT_ACTIVE", lateCancel.get("code").textValue());

        String artifactBase = "/api/v1/workspaces/ws_default/runs/"
                + run.get("id").textValue() + "/artifacts";
        JsonNode artifact = accepted(post(artifactBase + "/result-export", "{}", null), 201);
        assertEquals("RUN_RESULTS_CSV", artifact.get("type").textValue());
        assertEquals("READY", artifact.get("state").textValue());
        assertEquals(64, artifact.get("sha256").textValue().length());
        JsonNode artifactReplay = accepted(post(artifactBase + "/result-export", "{}", null), 200);
        assertEquals(artifact.get("id").textValue(), artifactReplay.get("id").textValue());
        JsonNode artifactList = accepted(get(artifactBase), 200);
        assertEquals(1, artifactList.size());

        String contentPath = artifact.at("/links/content").textValue();
        HttpResponse<String> csv = get(contentPath);
        assertEquals(200, csv.statusCode(), csv.body());
        assertTrue(csv.body().startsWith("item_id,state,attempts"));
        assertEquals(3, csv.body().lines().count());
        HttpRequest rangeRequest = HttpRequest.newBuilder(uri(contentPath))
                .header("Range", "bytes=0-6").GET().build();
        HttpResponse<String> range = client.send(rangeRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(206, range.statusCode(), range.body());
        assertEquals("item_id", range.body());
        assertTrue(range.headers().firstValue("Content-Range").orElseThrow().startsWith("bytes 0-6/"));

        jdbc.update("UPDATE artifact_record SET expires_at = created_at WHERE id = ?",
                artifact.get("id").textValue());
        JsonNode cleanup = accepted(post(
                "/api/v1/system/maintenance/artifacts/retention?limit=10", "{}", null), 200);
        assertEquals(1, cleanup.get("deleted").intValue());
        JsonNode deletedArtifact = accepted(get(artifact.at("/links/self").textValue()), 200);
        assertEquals("DELETED", deletedArtifact.get("state").textValue());
        assertEquals(409, get(contentPath).statusCode());

        HttpResponse<String> missingConfirmation = post(runPath,
                "{\"dryRun\":false,\"reason\":\"integration-test\"}", "live-no-confirm");
        assertEquals(409, missingConfirmation.statusCode());
        assertTrue(missingConfirmation.body().contains("LIVE_CONFIRMATION_REQUIRED"));

        JsonNode livePreview = accepted(post(
                "/api/v1/workspaces/ws_default/jobs/" + job.get("id").textValue() + "/run-confirmation",
                "{}", null), 200);
        assertEquals(2, livePreview.get("audienceCount").longValue());
        assertEquals(1, livePreview.get("targetConcurrency").intValue());
        String liveRequest = """
                {"dryRun":false,"reason":"integration-live","confirmationToken":"%s"}
                """.formatted(livePreview.get("confirmationToken").textValue());
        HttpResponse<String> conflictingReplay = post(runPath, liveRequest, "control-plane-test");
        assertEquals(409, conflictingReplay.statusCode());
        assertTrue(conflictingReplay.body().contains("IDEMPOTENCY_KEY_REUSED"));

        PROVIDER_SENDS.set(0);
        JsonNode liveRun = accepted(post(runPath, liveRequest, "live-run-test"), 202);
        JsonNode partial = awaitTerminalRun(liveRun.get("id").textValue());
        assertEquals("PARTIAL", partial.get("state").textValue());
        assertEquals(1, partial.at("/counters/failed").longValue());

        String retryBase = "/api/v1/workspaces/ws_default/runs/" + liveRun.get("id").textValue();
        JsonNode retryPreview = accepted(post(retryBase + "/retry-confirmation",
                "{\"states\":[\"FAILED\"]}", null), 200);
        assertEquals(1, retryPreview.get("itemCount").longValue());
        FAIL_BOB.set(false);
        JsonNode retryRun = accepted(post(retryBase + "/retries", """
                {"states":["FAILED"],"confirmationToken":"%s"}
                """.formatted(retryPreview.get("confirmationToken").textValue()), "retry-run-test"), 202);
        assertEquals(liveRun.get("id").textValue(), retryRun.get("sourceRunId").textValue());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM run_retry_item WHERE run_id = ?",
                Long.class, retryRun.get("id").textValue()));
        JsonNode retried = awaitTerminalRun(retryRun.get("id").textValue());
        assertEquals("SUCCEEDED", retried.get("state").textValue());
        assertEquals(1, retried.at("/counters/total").longValue());

        JsonNode overview = accepted(get("/api/v1/workspaces/ws_default/overview"), 200);
        assertTrue(overview.get("totalRuns").longValue() >= 3);
        assertTrue(overview.get("recent").isArray());

        JsonNode jobCopy = accepted(post("/api/v1/workspaces/ws_default/jobs/"
                + job.get("id").textValue() + "/copy", "{\"name\":\"Archived copy\"}", null), 201);
        JsonNode archivedJob = accepted(patch("/api/v1/workspaces/ws_default/jobs/"
                + jobCopy.get("id").textValue(), "{\"enabled\":false,\"archived\":true}"), 200);
        assertEquals("ARCHIVED", archivedJob.get("status").textValue());
        JsonNode disabledAccount = accepted(patch(accountPath, "{\"status\":\"DISABLED\"}"), 200);
        assertEquals("DISABLED", disabledAccount.get("status").textValue());
        JsonNode archivedAccount = accepted(patch(accountPath, "{\"status\":\"ARCHIVED\"}"), 200);
        assertEquals("ARCHIVED", archivedAccount.get("status").textValue());

        String eventsPath = "/api/v1/workspaces/ws_default/runs/" + run.get("id").textValue() + "/events";
        HttpRequest eventsRequest = HttpRequest.newBuilder(uri(eventsPath))
                .header("Accept", "text/event-stream").GET().build();
        HttpResponse<java.io.InputStream> stream = client.send(eventsRequest,
                HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(200, stream.statusCode());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream.body(), StandardCharsets.UTF_8))) {
            String eventFrame = reader.readLine() + "\n" + reader.readLine() + "\n" + reader.readLine();
            assertTrue(eventFrame.contains("id:1"));
            assertTrue(eventFrame.contains("event:RUN_CREATED"));
            assertTrue(eventFrame.contains("\"total\":2"));
        }
    }

    private JsonNode accepted(HttpResponse<String> response, int expectedStatus) throws Exception {
        assertEquals(expectedStatus, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private HttpResponse<String> post(String path, String body, String idempotencyKey) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> patch(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> multipart(String path, Map<String, String> fields,
                                           String fileName, String fileContent) throws Exception {
        String boundary = "wepush-" + UUID.randomUUID();
        StringBuilder body = new StringBuilder();
        fields.forEach((name, value) -> body.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n")
                .append(value).append("\r\n"));
        body.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(fileName).append("\"\r\n")
                .append("Content-Type: text/csv\r\n\r\n")
                .append(fileContent).append("\r\n--").append(boundary).append("--\r\n");
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode awaitTerminalRun(String runId) throws Exception {
        URI runUri = uri("/api/v1/workspaces/ws_default/runs/" + runId);
        for (int attempt = 0; attempt < 400; attempt++) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(runUri).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());
            JsonNode run = json.readTree(response.body());
            if (run.get("state").textValue().matches("SUCCEEDED|PARTIAL|FAILED|CANCELLED")) {
                return run;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Run did not reach a terminal state");
    }

    private JsonNode awaitRunState(String runId, String expected) throws Exception {
        URI runUri = uri("/api/v1/workspaces/ws_default/runs/" + runId);
        for (int attempt = 0; attempt < 100; attempt++) {
            JsonNode run = accepted(client.send(HttpRequest.newBuilder(runUri).GET().build(),
                    HttpResponse.BodyHandlers.ofString()), 200);
            if (expected.equals(run.get("state").textValue())) {
                return run;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Run did not reach state " + expected);
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
