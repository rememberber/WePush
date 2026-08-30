package com.fangxuele.wepush.next.agent.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.AgentId;
import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import com.fangxuele.wepush.next.agent.protocol.RemoteRunDocuments;
import com.fangxuele.wepush.next.agent.protocol.SecretEnvelopeCodec;
import com.fangxuele.wepush.next.agent.runtime.AgentJournal;
import com.fangxuele.wepush.next.agent.runtime.AgentJournalState;
import com.fangxuele.wepush.next.agent.runtime.AgentRuntime;
import com.fangxuele.wepush.next.agent.runtime.InMemoryAgentEventOutbox;
import com.fangxuele.wepush.next.agent.runtime.InMemoryAgentCompletionOutbox;
import com.fangxuele.wepush.next.agent.runtime.LeaseState;
import com.fangxuele.wepush.next.core.engine.DefaultExecutionEngine;
import com.fangxuele.wepush.next.provider.http.HttpProviderFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteAgentRunExecutorTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void retriesTransientMultipartPartFailuresAndStreamsOnlyTheRequestedSlice(
            @TempDir Path directory) throws Exception {
        Path file = directory.resolve("artifact.bin");
        Files.writeString(file, "0123456789", StandardCharsets.UTF_8);
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<String> received = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/part", exchange -> {
            received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (requests.getAndIncrement() == 0) {
                exchange.sendResponseHeaders(500, -1);
            } else {
                exchange.getResponseHeaders().add("ETag", "\"part-etag\"");
                exchange.sendResponseHeaders(200, -1);
            }
            exchange.close();
        });
        server.start();
        try {
            URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/part");
            String eTag = RemoteAgentRunExecutor.uploadFilePart(
                    HttpClient.newHttpClient(), file, 2, 4, url, Map.of(), 1);
            assertEquals("\"part-etag\"", eTag);
            assertEquals(2, requests.get());
            assertEquals("2345", received.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void replaysDurableBusinessEventsOnAuthorizedReconnectUntilAcknowledged() throws Exception {
        InMemoryAgentEventOutbox outbox = new InMemoryAgentEventOutbox();
        LeaseFence fence = new LeaseFence("lease-replay", "run-replay", 1, "replay-token");
        outbox.append(fence, List.of("event".getBytes(StandardCharsets.UTF_8)));
        BlockingQueue<AgentFrames.AgentToService> first = new LinkedBlockingQueue<>();
        BlockingQueue<AgentFrames.AgentToService> second = new LinkedBlockingQueue<>();

        try (AgentRuntime runtime = new AgentRuntime(new AgentId("test-agent"), "test", 1,
                new DefaultExecutionEngine(List.of(new HttpProviderFactory())), new InMemoryJournal());
             RemoteAgentRunExecutor executor = new RemoteAgentRunExecutor(
                     "test-agent", runtime, json, "", outbox)) {
            RemoteAgentRunExecutor.AgentFrameSender firstSender = first::add;
            executor.connected(firstSender, List.of(fence));
            AgentFrames.EventBatch initial = (AgentFrames.EventBatch) first.poll(1, TimeUnit.SECONDS).payload();
            executor.disconnected(firstSender);

            executor.connected(second::add, List.of(fence));
            AgentFrames.AgentToService replayedFrame = second.poll(1, TimeUnit.SECONDS);
            AgentFrames.EventBatch replayed = (AgentFrames.EventBatch) replayedFrame.payload();
            assertEquals(initial.firstEventSequence(), replayed.firstEventSequence());
            assertEquals(2, replayedFrame.sequence());
            assertTrue(executor.pendingEventBytes() > 0);

            executor.acknowledge(new AgentFrames.EventAck(fence,
                    replayed.firstEventSequence() + replayed.events().size() - 1L));
            assertEquals(0, executor.pendingEventBytes());
        }
    }

    @Test
    void convertsInterruptedRunningLeaseToUnknownCompletionAndWaitsForAck() throws Exception {
        LeaseFence fence = new LeaseFence("lease-crashed", "run-crashed", 3, "crash-token");
        InMemoryJournal journal = new InMemoryJournal();
        journal.save(new AgentJournalState(4, 2, Map.of(fence.leaseId(),
                new AgentJournalState.PersistedLease(fence, Instant.now().plusSeconds(30),
                        LeaseState.RUNNING, "spec-sha", "audience-sha", 7,
                        Instant.now().minusSeconds(5)))));
        BlockingQueue<AgentFrames.AgentToService> outbound = new LinkedBlockingQueue<>();

        try (AgentRuntime runtime = new AgentRuntime(new AgentId("test-agent"), "test", 1,
                new DefaultExecutionEngine(List.of(new HttpProviderFactory())), journal);
             RemoteAgentRunExecutor executor = new RemoteAgentRunExecutor("test-agent", runtime, json, "",
                     new InMemoryAgentEventOutbox(), new InMemoryAgentCompletionOutbox())) {
            executor.connected(outbound::add, List.of(fence));
            AgentFrames.RunCompleted completed = (AgentFrames.RunCompleted)
                    outbound.poll(1, TimeUnit.SECONDS).payload();
            RemoteRunDocuments.Summary summary = json.readValue(completed.summary(),
                    RemoteRunDocuments.Summary.class);
            assertEquals("FAILED", summary.finalState());
            assertEquals(7, summary.total());
            assertEquals(7, summary.unknown());
            assertEquals(LeaseState.RUNNING, journal.load().leases().get(fence.leaseId()).state());

            executor.acknowledge(new AgentFrames.RunCompletionAck(fence));
            assertEquals(LeaseState.COMPLETED,
                    journal.load().leases().get(fence.leaseId()).state());
        }
    }

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

    @Test
    void decryptsLeaseBoundSecretInMemoryForRealProviderExecution() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/notify", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        int port = server.getAddress().getPort();
        Instant createdAt = Instant.now();
        RemoteRunDocuments.ExecutionSpec spec = new RemoteRunDocuments.ExecutionSpec(
                "run_secret", "ws_default", "wepush.http", "0.1.0",
                "{\"baseUrl\":\"http://127.0.0.1:%d\",\"allowPrivateAddresses\":true,"
                        .formatted(port)
                        + "\"auth\":{\"type\":\"BEARER\",\"secret\":{"
                        + "\"namespace\":\"http\",\"name\":\"authorization\",\"version\":\"v1\"}}}",
                "{\"method\":\"POST\",\"path\":\"/notify\",\"bodyTemplate\":\"{}\"}",
                "{\"concurrency\":{\"minimum\":1,\"target\":1,\"maximum\":1}}",
                Map.of(), false, createdAt);
        RemoteRunDocuments.Audience audience = new RemoteRunDocuments.Audience(List.of(
                new RemoteRunDocuments.Recipient(0, "alice", "{\"mobile\":\"13000000000\"}")));
        byte[] specBytes = json.writeValueAsBytes(spec);
        byte[] audienceBytes = json.writeValueAsBytes(audience);
        server.createContext("/spec-secret", exchange -> respond(exchange, specBytes));
        server.createContext("/audience-secret", exchange -> respond(exchange, audienceBytes));
        server.start();

        InMemoryJournal journal = new InMemoryJournal();
        LeaseFence fence = new LeaseFence("lease_secret", "run_secret", 2, "secret-fence-token");
        BlockingQueue<AgentFrames.AgentToService> outbound = new LinkedBlockingQueue<>();
        try (AgentRuntime runtime = new AgentRuntime(new AgentId("test-agent"), "test", 1,
                new DefaultExecutionEngine(List.of(new HttpProviderFactory())), journal);
             RemoteAgentRunExecutor executor = new RemoteAgentRunExecutor("test-agent", runtime, json, "")) {
            SecretEnvelopeCodec codec = new SecretEnvelopeCodec();
            Instant expiresAt = Instant.now().plusSeconds(60);
            byte[] envelope;
            try (SecretEnvelopeCodec.SecretMaterial secret = new SecretEnvelopeCodec.SecretMaterial(
                    "http", "authorization", "v1", "agent-token".getBytes(StandardCharsets.UTF_8))) {
                envelope = codec.seal("test-agent", fence, expiresAt,
                        executor.secretEncryptionPublicKey(), List.of(secret));
            }
            assertFalse(new String(envelope, StandardCharsets.ISO_8859_1).contains("agent-token"));
            AgentFrames.LeaseOffer offer = new AgentFrames.LeaseOffer(fence, expiresAt,
                    "http://127.0.0.1:" + port + "/spec-secret", sha256(specBytes),
                    "http://127.0.0.1:" + port + "/audience-secret", sha256(audienceBytes), envelope);
            assertEquals(com.fangxuele.wepush.next.agent.runtime.InboundSequenceResult.ACCEPTED,
                    runtime.accept(new AgentFrames.ServiceToAgent(1, offer)));
            executor.offer(offer, outbound::add);

            AgentFrames.RunCompleted completed = null;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (completed == null && System.nanoTime() < deadline) {
                AgentFrames.AgentToService frame = outbound.poll(200, TimeUnit.MILLISECONDS);
                if (frame != null && frame.payload() instanceof AgentFrames.RunCompleted value) {
                    completed = value;
                }
            }
            assertNotNull(completed, "Agent did not report encrypted-secret run completion");
            assertEquals("SUCCEEDED", json.readValue(completed.summary(),
                    RemoteRunDocuments.Summary.class).finalState());
            assertEquals("Bearer agent-token", authorization.get());
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
