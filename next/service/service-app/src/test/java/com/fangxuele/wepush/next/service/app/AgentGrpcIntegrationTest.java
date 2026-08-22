package com.fangxuele.wepush.next.service.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.AgentId;
import com.fangxuele.wepush.next.agent.protocol.AgentProtoMapper;
import com.fangxuele.wepush.next.agent.protocol.ProviderCapability;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AgentGrpcIntegrationTest {
    private static final Path DATABASE = Path.of(System.getProperty("java.io.tmpdir"),
            "wepush-next-agent-control-" + UUID.randomUUID() + ".db");
    private static final Path MASTER_KEY = Path.of(System.getProperty("java.io.tmpdir"),
            "wepush-next-agent-key-" + UUID.randomUUID() + ".json");

    @DynamicPropertySource
    static void configuration(DynamicPropertyRegistry registry) {
        registry.add("wepush.database.path", DATABASE::toString);
        registry.add("wepush.secret.master-key-path", MASTER_KEY::toString);
        registry.add("wepush.agent.grpc.port", () -> "0");
        registry.add("wepush.agent.grpc.token", () -> "integration-agent-token");
        registry.add("wepush.agent.grpc.heartbeat-interval", () -> "PT1S");
        registry.add("server.shutdown", () -> "immediate");
    }

    @Autowired
    private AgentGrpcServer grpcServer;

    @LocalServerPort
    private int httpPort;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void registersHeartbeatsAndDisconnectsAuthenticatedAgent() throws Exception {
        ManagedChannel channel = NettyChannelBuilder.forAddress("127.0.0.1", grpcServer.localPort())
                .usePlaintext().build();
        try {
            Metadata metadata = new Metadata();
            metadata.put(Metadata.Key.of("x-wepush-agent-token", Metadata.ASCII_STRING_MARSHALLER),
                    "integration-agent-token");
            AgentControlServiceGrpc.AgentControlServiceStub stub =
                    AgentControlServiceGrpc.newStub(channel).withInterceptors(
                            MetadataUtils.newAttachHeadersInterceptor(metadata));
            CountDownLatch welcomed = new CountDownLatch(1);
            CountDownLatch completed = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            AtomicReference<ServiceToAgent> welcome = new AtomicReference<>();
            StreamObserver<AgentToService> requests = stub.connect(new StreamObserver<>() {
                @Override
                public void onNext(ServiceToAgent value) {
                    welcome.set(value);
                    welcomed.countDown();
                }

                @Override
                public void onError(Throwable throwable) {
                    failure.set(throwable);
                    completed.countDown();
                }

                @Override
                public void onCompleted() {
                    completed.countDown();
                }
            });

            requests.onNext(AgentProtoMapper.toProto(new AgentFrames.AgentToService(
                    new AgentId("integration-agent"), 1,
                    new AgentFrames.Hello("0.1.0-test", 1, 1, "TestOS", "amd64", "21",
                            4, 0, 0,
                            List.of(new ProviderCapability("wepush.http", "0.1.0", 1, 32))))));
            assertTrue(welcomed.await(5, TimeUnit.SECONDS), "Service did not send Welcome");
            assertEquals(1, AgentProtoMapper.fromProto(welcome.get()).sequence());

            requests.onNext(AgentProtoMapper.toProto(new AgentFrames.AgentToService(
                    new AgentId("integration-agent"), 2,
                    new AgentFrames.Heartbeat("READY", 1, 3, List.of()))));

            JsonNode online = awaitStatus("ONLINE");
            assertEquals(1, online.get("activeRuns").intValue());
            assertEquals(3, online.get("availableRuns").intValue());
            assertEquals(2, online.get("lastAgentSequence").longValue());
            assertEquals("wepush.http", online.at("/providers/0/providerId").textValue());

            requests.onCompleted();
            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertNull(failure.get());
            JsonNode offline = awaitStatus("OFFLINE");
            assertEquals(0, offline.get("activeRuns").intValue());
            assertEquals(0, offline.get("availableRuns").intValue());
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private JsonNode awaitStatus(String status) throws Exception {
        URI uri = URI.create("http://127.0.0.1:" + httpPort +
                "/api/v1/agents/integration-agent");
        JsonNode latest = null;
        for (int attempt = 0; attempt < 100; attempt++) {
            HttpResponse<String> response = http.send(HttpRequest.newBuilder(uri).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                latest = json.readTree(response.body());
                if (status.equals(latest.get("status").textValue())) return latest;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Agent did not reach " + status + ": " + latest);
    }
}
