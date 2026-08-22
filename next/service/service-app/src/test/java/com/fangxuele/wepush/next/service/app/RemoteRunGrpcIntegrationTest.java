package com.fangxuele.wepush.next.service.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.AgentId;
import com.fangxuele.wepush.next.agent.protocol.AgentProtoMapper;
import com.fangxuele.wepush.next.agent.protocol.ProviderCapability;
import com.fangxuele.wepush.next.agent.protocol.RemoteRunDocuments;
import com.fangxuele.wepush.next.agent.protocol.v1.AgentControlServiceGrpc;
import com.fangxuele.wepush.next.agent.protocol.v1.AgentToService;
import com.fangxuele.wepush.next.agent.protocol.v1.ServiceToAgent;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RemoteRunGrpcIntegrationTest {
    private static final String TOKEN = "remote-run-agent-token";
    private static final Path DATABASE = Path.of(System.getProperty("java.io.tmpdir"),
            "wepush-next-remote-run-" + UUID.randomUUID() + ".db");
    private static final Path MASTER_KEY = Path.of(System.getProperty("java.io.tmpdir"),
            "wepush-next-remote-key-" + UUID.randomUUID() + ".json");

    @DynamicPropertySource
    static void configuration(DynamicPropertyRegistry registry) {
        registry.add("wepush.database.path", DATABASE::toString);
        registry.add("wepush.secret.master-key-path", MASTER_KEY::toString);
        registry.add("wepush.execution.mode", () -> "remote");
        registry.add("wepush.agent.grpc.port", () -> "0");
        registry.add("wepush.agent.grpc.token", () -> TOKEN);
        registry.add("wepush.agent.grpc.heartbeat-interval", () -> "PT1S");
        registry.add("wepush.agent.lease-scan-interval", () -> "PT1H");
        registry.add("server.shutdown", () -> "immediate");
    }

    @Autowired
    private AgentGrpcServer grpcServer;

    @Autowired
    private JdbcTemplate jdbc;

    @LocalServerPort
    private int httpPort;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void leasesRunAcceptsCommandsDeduplicatesEventsAndFinalizesResults() throws Exception {
        ManagedChannel channel = NettyChannelBuilder.forAddress("127.0.0.1", grpcServer.localPort())
                .usePlaintext().build();
        try {
            Metadata metadata = new Metadata();
            metadata.put(Metadata.Key.of("x-wepush-agent-token", Metadata.ASCII_STRING_MARSHALLER), TOKEN);
            AgentControlServiceGrpc.AgentControlServiceStub stub = AgentControlServiceGrpc.newStub(channel)
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
            CountDownLatch welcomed = new CountDownLatch(1);
            BlockingQueue<AgentFrames.LeaseOffer> offers = new LinkedBlockingQueue<>();
            BlockingQueue<AgentFrames.RunCommand> commands = new LinkedBlockingQueue<>();
            BlockingQueue<AgentFrames.EventAck> eventAcks = new LinkedBlockingQueue<>();
            AtomicReference<Throwable> streamFailure = new AtomicReference<>();
            StreamObserver<AgentToService> requests = stub.connect(new StreamObserver<>() {
                @Override
                public void onNext(ServiceToAgent value) {
                    AgentFrames.ServicePayload payload = AgentProtoMapper.fromProto(value).payload();
                    if (payload instanceof AgentFrames.Welcome) welcomed.countDown();
                    if (payload instanceof AgentFrames.LeaseOffer offer) offers.add(offer);
                    if (payload instanceof AgentFrames.RunCommand command) commands.add(command);
                    if (payload instanceof AgentFrames.EventAck ack) eventAcks.add(ack);
                }

                @Override
                public void onError(Throwable throwable) {
                    streamFailure.set(throwable);
                }

                @Override
                public void onCompleted() {
                }
            });

            AgentId agentId = new AgentId("remote-integration-agent");
            requests.onNext(AgentProtoMapper.toProto(new AgentFrames.AgentToService(agentId, 1,
                    new AgentFrames.Hello("0.1.0-test", 1, 1, "TestOS", "amd64", "21",
                            2, 0, 0, List.of(new ProviderCapability(
                            "wepush.http", "0.1.0", 1, 32))))));
            assertTrue(welcomed.await(5, TimeUnit.SECONDS));

            String runId = createRemoteRun();
            AgentFrames.LeaseOffer offer = offers.poll(5, TimeUnit.SECONDS);
            assertNotNull(offer, "Service did not offer the pending run");
            assertEquals(runId, offer.fence().runId());

            HttpResponse<byte[]> unauthorized = http.send(HttpRequest.newBuilder(
                    uri("/internal/agent/v1/leases/" + offer.fence().leaseId() + "/execution-spec"))
                    .GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(401, unauthorized.statusCode());
            byte[] specBytes = internalDocument(offer.fence().leaseId(), "execution-spec");
            byte[] audienceBytes = internalDocument(offer.fence().leaseId(), "audience");
            assertHash(specBytes, offer.executionSpecSha256());
            assertHash(audienceBytes, offer.audienceSha256());
            assertEquals(runId, json.readValue(specBytes,
                    RemoteRunDocuments.ExecutionSpec.class).runId());
            assertEquals(1, json.readValue(audienceBytes,
                    RemoteRunDocuments.Audience.class).recipients().size());

            requests.onNext(AgentProtoMapper.toProto(new AgentFrames.AgentToService(agentId, 2,
                    new AgentFrames.LeaseAck(offer.fence()))));
            awaitRunState(runId, "RUNNING");

            JsonNode commandResponse = accepted(post("/api/v1/workspaces/ws_default/runs/" + runId
                    + "/commands/concurrency", "{\"target\":2}", "remote-concurrency"), 202);
            assertEquals("REMOTE_COMMAND_DELIVERED", commandResponse.get("code").textValue());
            AgentFrames.RunCommand command = commands.poll(5, TimeUnit.SECONDS);
            assertNotNull(command);
            assertEquals("CONCURRENCY", command.type());
            requests.onNext(AgentProtoMapper.toProto(new AgentFrames.AgentToService(agentId, 3,
                    new AgentFrames.CommandAck(command.commandId(), offer.fence(),
                            "ACCEPTED", "CONCURRENCY_CHANGED"))));

            Instant completedAt = Instant.now();
            RemoteRunDocuments.Report started = RemoteRunDocuments.Report.event(
                    new RemoteRunDocuments.Event(runId, 1, "RUN_STARTED", completedAt,
                            Map.of("mode", "remote-test")));
            RemoteRunDocuments.Report result = RemoteRunDocuments.Report.results(List.of(
                    new RemoteRunDocuments.ItemResult(runId, "alice", 1, "SUCCEEDED",
                            "DRY_RUN", "", "", completedAt, Map.of("agent", agentId.value()))));
            AgentFrames.EventBatch batch = new AgentFrames.EventBatch(offer.fence(), 1,
                    List.of(json.writeValueAsBytes(started), json.writeValueAsBytes(result)));
            requests.onNext(AgentProtoMapper.toProto(new AgentFrames.AgentToService(agentId, 4, batch)));
            assertEquals(2, requireAck(eventAcks).lastEventSequence());

            requests.onNext(AgentProtoMapper.toProto(new AgentFrames.AgentToService(agentId, 5, batch)));
            assertEquals(2, requireAck(eventAcks).lastEventSequence());
            Integer resultCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM run_item_result WHERE run_id = ?", Integer.class, runId);
            assertEquals(1, resultCount);

            RemoteRunDocuments.Summary summary = new RemoteRunDocuments.Summary(runId, "SUCCEEDED",
                    1, 1, 0, 0, 0, 0, 0, completedAt.minusMillis(1), completedAt);
            requests.onNext(AgentProtoMapper.toProto(new AgentFrames.AgentToService(agentId, 6,
                    new AgentFrames.RunCompleted(offer.fence(), json.writeValueAsBytes(summary), List.of()))));

            JsonNode completed = awaitRunState(runId, "SUCCEEDED");
            assertEquals(1, completed.at("/counters/succeeded").intValue());
            JsonNode items = accepted(get("/api/v1/workspaces/ws_default/runs/" + runId + "/items"), 200);
            assertEquals("alice", items.at("/items/0/itemId").textValue());
            assertEquals("SUCCEEDED", items.at("/items/0/state").textValue());
            assertEquals("COMPLETED", jdbc.queryForObject(
                    "SELECT status FROM agent_lease WHERE id = ?", String.class, offer.fence().leaseId()));
            assertEquals(2L, jdbc.queryForObject(
                    "SELECT last_event_sequence FROM agent_lease WHERE id = ?", Long.class,
                    offer.fence().leaseId()));
            assertEquals(null, streamFailure.get());
            requests.onCompleted();
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private String createRemoteRun() throws Exception {
        JsonNode account = accepted(post("/api/v1/workspaces/ws_default/accounts", """
                {"name":"Remote HTTP","providerId":"wepush.http","providerVersion":"0.1.0",
                 "configuration":{"baseUrl":"https://example.com","auth":{"type":"NONE"}}}
                """, null), 201);
        JsonNode message = accepted(post("/api/v1/workspaces/ws_default/messages", """
                {"name":"Remote message","providerId":"wepush.http","providerVersion":"0.1.0",
                 "content":{"method":"POST","path":"/notify","bodyTemplate":"{}"}}
                """, null), 201);
        JsonNode audience = accepted(post("/api/v1/workspaces/ws_default/audiences", """
                {"name":"Remote audience","recipients":[
                  {"itemId":"alice","fields":{"mobile":"13000000000"}}]}
                """, null), 201);
        JsonNode job = accepted(post("/api/v1/workspaces/ws_default/jobs", """
                {"name":"Remote job","accountId":"%s","messageId":"%s","audienceId":"%s",
                 "policies":{"concurrency":{"minimum":1,"target":1,"maximum":2}},"enabled":true}
                """.formatted(account.get("id").textValue(), message.get("id").textValue(),
                audience.get("id").textValue()), null), 201);
        JsonNode run = accepted(post("/api/v1/workspaces/ws_default/jobs/"
                + job.get("id").textValue() + "/runs",
                "{\"dryRun\":true,\"policyOverrides\":{},\"reason\":\"remote-test\"}",
                "remote-run"), 202);
        return run.get("id").textValue();
    }

    private AgentFrames.EventAck requireAck(BlockingQueue<AgentFrames.EventAck> acks)
            throws InterruptedException {
        AgentFrames.EventAck ack = acks.poll(5, TimeUnit.SECONDS);
        assertNotNull(ack, "Service did not acknowledge Agent events");
        return ack;
    }

    private byte[] internalDocument(String leaseId, String kind) throws Exception {
        HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder(
                        uri("/internal/agent/v1/leases/" + leaseId + "/" + kind))
                .header("x-wepush-agent-token", TOKEN).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
        return response.body();
    }

    private void assertHash(byte[] body, String expected) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(body);
        assertTrue(MessageDigest.isEqual(java.util.HexFormat.of().formatHex(digest)
                .getBytes(StandardCharsets.US_ASCII), expected.getBytes(StandardCharsets.US_ASCII)));
    }

    private JsonNode awaitRunState(String runId, String expected) throws Exception {
        String path = "/api/v1/workspaces/ws_default/runs/" + runId;
        JsonNode last = null;
        for (int attempt = 0; attempt < 200; attempt++) {
            last = accepted(get(path), 200);
            if (expected.equals(last.get("state").textValue())) return last;
            Thread.sleep(20);
        }
        throw new AssertionError("Run did not reach " + expected + ": " + last);
    }

    private HttpResponse<String> post(String path, String body, String idempotencyKey) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json");
        if (idempotencyKey != null) request.header("Idempotency-Key", idempotencyKey);
        return http.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode accepted(HttpResponse<String> response, int expected) throws Exception {
        assertEquals(expected, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + httpPort + path);
    }
}
