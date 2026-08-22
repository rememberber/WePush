package com.fangxuele.wepush.next.agent.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import com.fangxuele.wepush.next.agent.protocol.RemoteRunDocuments;
import com.fangxuele.wepush.next.agent.runtime.AgentRuntime;
import com.fangxuele.wepush.next.core.api.ArtifactSink;
import com.fangxuele.wepush.next.core.api.CommandResult;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.ExecutionPorts;
import com.fangxuele.wepush.next.core.api.ItemResult;
import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RecipientSource;
import com.fangxuele.wepush.next.core.api.RecipientValue;
import com.fangxuele.wepush.next.core.api.ResultSink;
import com.fangxuele.wepush.next.core.api.RunCommand;
import com.fangxuele.wepush.next.core.api.RunEvent;
import com.fangxuele.wepush.next.core.api.RunEventSink;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.core.api.RunHandle;
import com.fangxuele.wepush.next.core.api.RunSummary;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class RemoteAgentRunExecutor implements AutoCloseable {
    private static final int MAXIMUM_DOCUMENT_BYTES = 64 * 1024 * 1024;

    private final AgentRuntime runtime;
    private final ObjectMapper mapper;
    private final String token;
    private final HttpClient http;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    RemoteAgentRunExecutor(AgentRuntime runtime, ObjectMapper mapper, String token) {
        this.runtime = runtime;
        this.mapper = mapper;
        this.token = token == null ? "" : token;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    void offer(AgentFrames.LeaseOffer offer, AgentFrameSender sender) {
        executor.execute(() -> execute(offer, sender));
    }

    void command(AgentFrames.RunCommand frame, AgentFrameSender sender) {
        executor.execute(() -> {
            try {
                RunCommand command = command(frame);
                CommandResult result = runtime.command(frame.fence(), command);
                sender.send(runtime.commandAcknowledged(frame.commandId(), frame.fence(),
                        result.status().name(), result.code() + ":" + result.message()));
            } catch (RuntimeException problem) {
                sender.send(runtime.commandAcknowledged(frame.commandId(), frame.fence(),
                        "REJECTED", safeMessage(problem)));
            }
        });
    }

    private void execute(AgentFrames.LeaseOffer offer, AgentFrameSender sender) {
        Instant started = Instant.now();
        RemoteRunDocuments.Audience audience = null;
        boolean acknowledged = false;
        try {
            byte[] specBytes = download(offer.executionSpecUrl());
            byte[] audienceBytes = download(offer.audienceUrl());
            verify(specBytes, offer.executionSpecSha256(), "execution specification");
            verify(audienceBytes, offer.audienceSha256(), "audience");
            RemoteRunDocuments.ExecutionSpec document = mapper.readValue(
                    specBytes, RemoteRunDocuments.ExecutionSpec.class);
            audience = mapper.readValue(audienceBytes, RemoteRunDocuments.Audience.class);
            if (!offer.fence().runId().equals(document.runId())) {
                throw new IllegalArgumentException("lease and execution specification run IDs differ");
            }

            RunExecutionSpec spec = specification(document);
            RemoteExecution execution = new RemoteExecution(offer.fence(), audience, sender);
            sender.send(runtime.acknowledge(offer.fence()));
            acknowledged = true;
            RunHandle handle = runtime.start(offer.fence(), spec, execution.ports());
            RemoteRunDocuments.Audience acceptedAudience = audience;
            handle.completion().whenComplete((summary, failure) -> {
                if (failure == null) execution.complete(summary);
                else execution.fail(started, acceptedAudience.recipients().size(), failure);
            });
        } catch (IOException | InterruptedException problem) {
            if (problem instanceof InterruptedException) Thread.currentThread().interrupt();
            failBeforeStart(offer.fence(), sender, started,
                    audience == null ? 0 : audience.recipients().size(), acknowledged, problem);
        } catch (RuntimeException problem) {
            failBeforeStart(offer.fence(), sender, started,
                    audience == null ? 0 : audience.recipients().size(), acknowledged, problem);
        }
    }

    private RunExecutionSpec specification(RemoteRunDocuments.ExecutionSpec value)
            throws JsonProcessingException {
        Map<?, ?> policies = mapper.readValue(value.policiesJson(), Map.class);
        return new RunExecutionSpec(value.runId(),
                new ProviderRef(value.providerId(), value.providerVersion()),
                config(value.accountConfigurationJson(), "account"),
                config(value.messageContentJson(), "message"),
                ExecutionPolicyReader.read(policies), value.attributes(), value.dryRun(), value.createdAt());
    }

    private ConfigDocument config(String value, String kind) {
        return new ConfigDocument("wepush/run-snapshot/" + kind, "1",
                ConfigDocument.JSON_MEDIA_TYPE, value.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] download(String url) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30)).GET();
        if (!token.isBlank()) builder.header("x-wepush-agent-token", token);
        HttpResponse<byte[]> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("lease document download returned HTTP " + response.statusCode());
        }
        if (response.body().length > MAXIMUM_DOCUMENT_BYTES) {
            throw new IOException("lease document exceeds the Agent size limit");
        }
        return response.body();
    }

    private void failBeforeStart(LeaseFence fence, AgentFrameSender sender, Instant started,
                                 long total, boolean acknowledged, Throwable failure) {
        if (acknowledged) {
            Instant ended = Instant.now();
            RemoteRunDocuments.Summary summary = new RemoteRunDocuments.Summary(fence.runId(), "FAILED",
                    total, 0, 0, 0, total, 0, 0, started, ended);
            sender.send(runtime.completed(fence, encode(summary), List.of()));
        }
        System.err.printf("Remote run %s could not start: %s%n", fence.runId(), safeMessage(failure));
    }

    private RunCommand command(AgentFrames.RunCommand frame) {
        Map<String, Object> payload;
        try {
            payload = frame.payload().length == 0 ? Map.of()
                    : mapper.readValue(frame.payload(), new TypeReference<>() { });
        } catch (IOException problem) {
            throw new IllegalArgumentException("run command payload is invalid", problem);
        }
        return switch (frame.type()) {
            case "PAUSE" -> new RunCommand.PauseRun(frame.commandId());
            case "RESUME" -> new RunCommand.ResumeRun(frame.commandId());
            case "CANCEL" -> new RunCommand.CancelRun(frame.commandId(),
                    String.valueOf(payload.getOrDefault("reason", "cancelled remotely")));
            case "CONCURRENCY" -> new RunCommand.ChangeConcurrency(frame.commandId(),
                    ((Number) payload.get("target")).intValue());
            default -> throw new IllegalArgumentException("unsupported remote run command: " + frame.type());
        };
    }

    private byte[] encode(Object value) {
        try {
            return mapper.writeValueAsBytes(value);
        } catch (JsonProcessingException problem) {
            throw new IllegalStateException("Agent report cannot be encoded", problem);
        }
    }

    private static void verify(byte[] value, String expected, String name) {
        String actual = sha256(value);
        if (expected == null || !MessageDigest.isEqual(actual.getBytes(StandardCharsets.US_ASCII),
                expected.toLowerCase().getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException(name + " SHA-256 does not match the lease offer");
        }
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String safeMessage(Throwable value) {
        String message = value.getMessage();
        return message == null || message.isBlank() ? value.getClass().getSimpleName() : message;
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    @FunctionalInterface
    interface AgentFrameSender {
        void send(AgentFrames.AgentToService frame);
    }

    private final class RemoteExecution {
        private final LeaseFence fence;
        private final List<RecipientRecord> recipients;
        private final AgentFrameSender sender;
        private long reportSequence;

        private RemoteExecution(LeaseFence fence, RemoteRunDocuments.Audience audience,
                                AgentFrameSender sender) throws JsonProcessingException {
            this.fence = fence;
            this.sender = sender;
            List<RecipientRecord> converted = new ArrayList<>(audience.recipients().size());
            for (RemoteRunDocuments.Recipient recipient : audience.recipients()) {
                converted.add(recipient(recipient));
            }
            this.recipients = List.copyOf(converted);
        }

        private ExecutionPorts ports() {
            return new ExecutionPorts(new ListRecipientSource(recipients), ref -> {
                throw new IllegalStateException(
                        "Remote secret envelope encryption is not enabled for this Agent");
            }, new RemoteResultSink(), ArtifactSink.none(), new RemoteEventSink(), ExecutionClock.system());
        }

        private synchronized void report(RemoteRunDocuments.Report report) {
            long sequence = ++reportSequence;
            sender.send(runtime.events(fence, sequence, List.of(encode(report))));
        }

        private void complete(RunSummary source) {
            RemoteRunDocuments.Summary summary = new RemoteRunDocuments.Summary(source.runId(),
                    source.finalState().name(), source.total(), source.succeeded(), source.failed(),
                    source.unknown(), source.unsent(), source.skipped(), source.retried(),
                    source.startedAt(), source.endedAt());
            sender.send(runtime.completed(fence, encode(summary), source.artifacts().stream()
                    .map(value -> value.artifactId()).toList()));
        }

        private void fail(Instant started, long total, Throwable failure) {
            Instant ended = Instant.now();
            RemoteRunDocuments.Summary summary = new RemoteRunDocuments.Summary(fence.runId(), "FAILED",
                    total, 0, 0, 0, total, 0, 0, started, ended);
            sender.send(runtime.completed(fence, encode(summary), List.of()));
            System.err.printf("Remote run %s failed: %s%n", fence.runId(), safeMessage(failure));
        }

        private RecipientRecord recipient(RemoteRunDocuments.Recipient source)
                throws JsonProcessingException {
            Map<String, Object> raw = mapper.readValue(source.fieldsJson(), new TypeReference<>() { });
            Map<String, RecipientValue> fields = new LinkedHashMap<>();
            raw.forEach((key, value) -> fields.put(key, recipientValue(value)));
            return new RecipientRecord(source.itemId(), source.sequence(), fields);
        }

        private RecipientValue recipientValue(Object value) {
            if (value == null) return RecipientValue.NullValue.INSTANCE;
            if (value instanceof String text) return new RecipientValue.TextValue(text);
            if (value instanceof Number number) {
                return new RecipientValue.NumberValue(new BigDecimal(number.toString()));
            }
            if (value instanceof Boolean bool) return new RecipientValue.BooleanValue(bool);
            try {
                return new RecipientValue.TextValue(mapper.writeValueAsString(value));
            } catch (JsonProcessingException problem) {
                throw new IllegalArgumentException("recipient value cannot be encoded", problem);
            }
        }

        private final class RemoteResultSink implements ResultSink {
            @Override
            public void append(List<ItemResult> batch) {
                if (batch == null || batch.isEmpty()) return;
                report(RemoteRunDocuments.Report.results(batch.stream().map(value ->
                        new RemoteRunDocuments.ItemResult(value.runId(), value.itemId(), value.attempts(),
                                value.state().name(), value.providerCode(), value.diagnostic(),
                                value.externalRequestId(), value.completedAt(), value.metadata())).toList()));
            }

            @Override
            public void flush() {
            }
        }

        private final class RemoteEventSink implements RunEventSink {
            @Override
            public void append(RunEvent value) {
                report(RemoteRunDocuments.Report.event(new RemoteRunDocuments.Event(value.runId(),
                        value.sequence(), value.type().name(), value.occurredAt(), value.data())));
            }

            @Override
            public void flush() {
            }
        }
    }

    private static final class ListRecipientSource implements RecipientSource {
        private final List<RecipientRecord> recipients;
        private int offset;

        private ListRecipientSource(List<RecipientRecord> recipients) {
            this.recipients = recipients;
        }

        @Override
        public long totalCount() {
            return recipients.size();
        }

        @Override
        public synchronized List<RecipientRecord> nextBatch(int maximumSize) {
            if (offset >= recipients.size()) return List.of();
            int end = Math.min(recipients.size(), offset + maximumSize);
            List<RecipientRecord> batch = new ArrayList<>(recipients.subList(offset, end));
            offset = end;
            return batch;
        }
    }
}
