package com.fangxuele.wepush.next.agent.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.AgentId;
import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import com.fangxuele.wepush.next.agent.protocol.RemoteRunDocuments;
import com.fangxuele.wepush.next.agent.runtime.AgentJournal;
import com.fangxuele.wepush.next.agent.runtime.AgentJournalState;
import com.fangxuele.wepush.next.agent.runtime.AgentRuntime;
import com.fangxuele.wepush.next.core.engine.DefaultExecutionEngine;
import com.fangxuele.wepush.next.provider.http.HttpProviderFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteAgentRunExecutorTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void downloadsVerifiedSnapshotExecutesCoreAndReportsResults() throws Exception {
        Instant createdAt = Instant.parse("2026-08-22T00:00:00Z");
        RemoteRunDocuments.ExecutionSpec spec = new RemoteRunDocuments.ExecutionSpec(
                "run_remote", "ws_default", "wepush.http", "0.1.0",
                "{\"baseUrl\":\"https://example.com\",\"auth\":{\"type\":\"NONE\"}}",
                "{\"method\":\"POST\",\"path\":\"/notify\",\"bodyTemplate\":\"{}\"}",
                "{\"concurrency\":{\"minimum\":1,\"target\":1,\"maximum\":2}}",
                Map.of("test", "remote"), true, createdAt);
        RemoteRunDocuments.Audience audience = new RemoteRunDocuments.Audience(List.of(
                new RemoteRunDocuments.Recipient(0, "alice", "{\"mobile\":\"13000000000\"}"),
                new RemoteRunDocuments.Recipient(1, "bob", "{\"mobile\":\"13100000000\"}")));
        byte[] specBytes = json.writeValueAsBytes(spec);
        byte[] audienceBytes = json.writeValueAsBytes(audience);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/spec", exchange -> respond(exchange, specBytes));
        server.createContext("/audience", exchange -> respond(exchange, audienceBytes));
        server.start();

        InMemoryJournal journal = new InMemoryJournal();
        LeaseFence fence = new LeaseFence("lease_remote", "run_remote", 1, "fence-token");
        AgentFrames.LeaseOffer offer = new AgentFrames.LeaseOffer(fence, Instant.now().plusSeconds(60),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/spec", sha256(specBytes),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/audience",
                sha256(audienceBytes), new byte[0]);
        BlockingQueue<AgentFrames.AgentToService> outbound = new LinkedBlockingQueue<>();

        try (AgentRuntime runtime = new AgentRuntime(new AgentId("test-agent"), "test", 1,
                new DefaultExecutionEngine(List.of(new HttpProviderFactory())), journal);
             RemoteAgentRunExecutor executor = new RemoteAgentRunExecutor(runtime, json, "")) {
            assertEquals(com.fangxuele.wepush.next.agent.runtime.InboundSequenceResult.ACCEPTED,
                    runtime.accept(new AgentFrames.ServiceToAgent(1, offer)));
            executor.offer(offer, outbound::add);

            List<AgentFrames.AgentPayload> payloads = new ArrayList<>();
            AgentFrames.RunCompleted completed = null;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (completed == null && System.nanoTime() < deadline) {
                AgentFrames.AgentToService frame = outbound.poll(200, TimeUnit.MILLISECONDS);
                if (frame == null) continue;
                payloads.add(frame.payload());
                if (frame.payload() instanceof AgentFrames.RunCompleted value) completed = value;
            }

            assertNotNull(completed, "Agent did not report run completion");
            assertTrue(payloads.stream().anyMatch(AgentFrames.LeaseAck.class::isInstance));
            assertTrue(payloads.stream().anyMatch(AgentFrames.EventBatch.class::isInstance));
            RemoteRunDocuments.Summary summary = json.readValue(completed.summary(),
                    RemoteRunDocuments.Summary.class);
            assertEquals("SUCCEEDED", summary.finalState());
            assertEquals(2, summary.total());
            assertEquals(2, summary.succeeded());

            List<RemoteRunDocuments.Report> reports = payloads.stream()
                    .filter(AgentFrames.EventBatch.class::isInstance)
                    .map(AgentFrames.EventBatch.class::cast)
                    .flatMap(value -> value.events().stream())
                    .map(this::report)
                    .toList();
            assertTrue(reports.stream().filter(value -> "RESULTS".equals(value.kind()))
                    .flatMap(value -> value.results().stream())
                    .allMatch(value -> "SUCCEEDED".equals(value.state())));
            assertEquals(2, reports.stream().filter(value -> "RESULTS".equals(value.kind()))
                    .flatMap(value -> value.results().stream()).count());
        } finally {
            server.stop(0);
        }
    }

    private RemoteRunDocuments.Report report(byte[] value) {
        try {
            return json.readValue(value, RemoteRunDocuments.Report.class);
        } catch (IOException problem) {
            throw new AssertionError(problem);
        }
    }

    private static void respond(HttpExchange exchange, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static final class InMemoryJournal implements AgentJournal {
        private AgentJournalState state = AgentJournalState.empty();

        @Override
        public AgentJournalState load() {
            return state;
        }

        @Override
        public void save(AgentJournalState state) {
            this.state = state;
        }
    }
}
