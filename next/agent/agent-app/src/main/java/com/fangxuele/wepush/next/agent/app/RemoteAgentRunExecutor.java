package com.fangxuele.wepush.next.agent.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import com.fangxuele.wepush.next.agent.protocol.RemoteRunDocuments;
import com.fangxuele.wepush.next.agent.protocol.SecretEnvelopeCodec;
import com.fangxuele.wepush.next.agent.runtime.AgentEventOutbox;
import com.fangxuele.wepush.next.agent.runtime.AgentCompletionOutbox;
import com.fangxuele.wepush.next.agent.runtime.AgentRuntime;
import com.fangxuele.wepush.next.agent.runtime.InMemoryAgentCompletionOutbox;
import com.fangxuele.wepush.next.agent.runtime.InMemoryAgentEventOutbox;
import com.fangxuele.wepush.next.core.api.ArtifactSink;
import com.fangxuele.wepush.next.core.api.ArtifactRef;
import com.fangxuele.wepush.next.core.api.CommandResult;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.ExecutionPorts;
import com.fangxuele.wepush.next.core.api.ItemResult;
import com.fangxuele.wepush.next.core.api.InMemorySecretValue;
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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;

final class RemoteAgentRunExecutor implements AutoCloseable {
    private static final int MAXIMUM_DOCUMENT_BYTES = 64 * 1024 * 1024;

    private final AgentRuntime runtime;
    private final ObjectMapper mapper;
    private final String token;
    private final String agentId;
    private final SecretEnvelopeCodec secretEnvelopes = new SecretEnvelopeCodec();
    private final KeyPair secretEncryptionKey;
    private final AgentEventOutbox eventOutbox;
    private final AgentCompletionOutbox completionOutbox;
    private final HttpClient http;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicReference<AgentFrameSender> activeSender = new AtomicReference<>();
    private final Object eventPumpLock = new Object();
    private final Map<LeaseFence, Long> transmittedEventSequences = new HashMap<>();
    private final Set<LeaseFence> resumableFences = new HashSet<>();
    private final Set<LeaseFence> transmittedCompletions = new HashSet<>();

    RemoteAgentRunExecutor(AgentRuntime runtime, ObjectMapper mapper, String token) {
        this("local-agent", runtime, mapper, token, new InMemoryAgentEventOutbox(),
                new InMemoryAgentCompletionOutbox());
    }

    RemoteAgentRunExecutor(String agentId, AgentRuntime runtime, ObjectMapper mapper, String token) {
        this(agentId, runtime, mapper, token, new InMemoryAgentEventOutbox(),
                new InMemoryAgentCompletionOutbox());
    }

    RemoteAgentRunExecutor(String agentId, AgentRuntime runtime, ObjectMapper mapper, String token,
                           AgentEventOutbox eventOutbox) {
        this(agentId, runtime, mapper, token, eventOutbox, new InMemoryAgentCompletionOutbox());
    }

    RemoteAgentRunExecutor(String agentId, AgentRuntime runtime, ObjectMapper mapper, String token,
                           AgentEventOutbox eventOutbox, AgentCompletionOutbox completionOutbox) {
        if (agentId == null || agentId.isBlank()) throw new IllegalArgumentException("agentId is required");
        this.agentId = agentId;
        this.runtime = runtime;
        this.mapper = mapper;
        this.token = token == null ? "" : token;
        this.eventOutbox = eventOutbox;
        this.completionOutbox = completionOutbox;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        this.secretEncryptionKey = secretEnvelopes.generateRecipientKeyPair();
    }

    String secretEncryptionPublicKey() {
        return secretEnvelopes.encodePublicKey(secretEncryptionKey.getPublic());
    }

    void offer(AgentFrames.LeaseOffer offer, AgentFrameSender sender) {
        synchronized (eventPumpLock) {
            resumableFences.add(offer.fence());
        }
        connected(sender, Set.copyOf(resumableFences));
        executor.execute(() -> execute(offer, sender));
    }

    void command(AgentFrames.RunCommand frame, AgentFrameSender sender) {
        synchronized (eventPumpLock) {
            resumableFences.add(frame.fence());
        }
        connected(sender, Set.copyOf(resumableFences));
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

    void connected(AgentFrameSender sender) {
        connected(sender, eventOutbox.pending().stream().map(AgentEventOutbox.PendingBatch::fence)
                .collect(java.util.stream.Collectors.toSet()));
    }

    void connected(AgentFrameSender sender, java.util.Collection<LeaseFence> resumable) {
        synchronized (eventPumpLock) {
            if (activeSender.get() != sender) transmittedEventSequences.clear();
            if (activeSender.get() != sender) transmittedCompletions.clear();
            resumableFences.clear();
            resumableFences.addAll(resumable);
            activeSender.set(sender);
            transmitPendingEvents();
            recoverInterruptedRuns();
            transmitPendingCompletions();
        }
    }

    void disconnected(AgentFrameSender sender) {
        synchronized (eventPumpLock) {
            if (activeSender.compareAndSet(sender, null)) {
                transmittedEventSequences.clear();
                transmittedCompletions.clear();
            }
        }
    }

    void acknowledge(AgentFrames.EventAck acknowledgement) {
        eventOutbox.acknowledge(acknowledgement);
        synchronized (eventPumpLock) {
            transmittedEventSequences.computeIfPresent(acknowledgement.fence(), (_fence, sent) ->
                    Math.max(sent, acknowledgement.lastEventSequence()));
            transmitPendingEvents();
        }
    }

    void acknowledge(AgentFrames.RunCompletionAck acknowledgement) {
        synchronized (eventPumpLock) {
            completionOutbox.acknowledge(acknowledgement.fence());
            transmittedCompletions.remove(acknowledgement.fence());
            runtime.recoveryCompleted(acknowledgement.fence());
        }
    }

    long pendingEventBytes() {
        return eventOutbox.sizeBytes();
    }

    private void transmitPendingEvents() {
        AgentFrameSender sender = activeSender.get();
        if (sender == null) return;
        for (AgentEventOutbox.PendingBatch batch : eventOutbox.pending()) {
            if (!resumableFences.contains(batch.fence())) continue;
            long sent = transmittedEventSequences.getOrDefault(batch.fence(), 0L);
            if (batch.lastEventSequence() <= sent) continue;
            try {
                sender.send(runtime.events(batch.fence(), batch.firstEventSequence(), batch.events()));
                transmittedEventSequences.put(batch.fence(), batch.lastEventSequence());
            } catch (RuntimeException connectionFailure) {
                activeSender.compareAndSet(sender, null);
                transmittedEventSequences.clear();
                return;
            }
        }
    }

    private void recoverInterruptedRuns() {
        Instant endedAt = Instant.now();
        for (AgentRuntime.RecoveryRun recovered : runtime.recoveryRuns()) {
            if (!resumableFences.contains(recovered.fence())) continue;
            long total = recovered.totalRecipients();
            boolean mayHaveCalledProvider = recovered.previousState()
                    == com.fangxuele.wepush.next.agent.runtime.LeaseState.RUNNING;
            RemoteRunDocuments.Summary summary = new RemoteRunDocuments.Summary(
                    recovered.fence().runId(), "FAILED", total, 0, 0,
                    mayHaveCalledProvider ? total : 0,
                    mayHaveCalledProvider ? 0 : total, 0, 0,
                    recovered.startedAt() == null ? endedAt : recovered.startedAt(), endedAt);
            completionOutbox.put(recovered.fence(), encode(summary), List.of());
        }
    }

    private void queueCompletion(LeaseFence fence, byte[] summary, List<String> artifacts) {
        completionOutbox.put(fence, summary, artifacts);
        synchronized (eventPumpLock) {
            transmitPendingCompletions();
        }
    }

    private void transmitPendingCompletions() {
        AgentFrameSender sender = activeSender.get();
        if (sender == null) return;
        for (AgentCompletionOutbox.PendingCompletion completion : completionOutbox.pending()) {
            if (!resumableFences.contains(completion.fence())
                    || transmittedCompletions.contains(completion.fence())) continue;
            try {
                sender.send(runtime.completed(completion.fence(), completion.summary(),
                        completion.artifactReferences()));
                transmittedCompletions.add(completion.fence());
            } catch (RuntimeException connectionFailure) {
                activeSender.compareAndSet(sender, null);
                transmittedEventSequences.clear();
                transmittedCompletions.clear();
                return;
            }
        }
    }

    private void execute(AgentFrames.LeaseOffer offer, AgentFrameSender sender) {
        Instant started = Instant.now();
        RemoteRunDocuments.Audience audience = null;
        SecretEnvelopeCodec.OpenedSecrets openedSecrets = null;
        RemoteExecution execution = null;
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

            if (offer.secretEnvelope().length > 0) {
                openedSecrets = secretEnvelopes.open(agentId, offer.fence(), offer.expiresAt(),
                        Instant.now(), secretEncryptionKey.getPrivate(), offer.secretEnvelope());
            }

            RunExecutionSpec spec = specification(document);
            execution = new RemoteExecution(offer.fence(), audience, sender, openedSecrets,
                    serviceBaseUrl(offer.executionSpecUrl()));
            openedSecrets = null;
            runtime.prepared(offer.fence(), offer.executionSpecSha256(), offer.audienceSha256(),
                    audience.recipients().size(), started);
            sender.send(runtime.acknowledge(offer.fence()));
            acknowledged = true;
            RunHandle handle = runtime.start(offer.fence(), spec, execution.ports());
            RemoteRunDocuments.Audience acceptedAudience = audience;
            RemoteExecution acceptedExecution = execution;
            handle.completion().whenComplete((summary, failure) -> {
                if (failure == null) acceptedExecution.complete(summary);
                else acceptedExecution.fail(started, acceptedAudience.recipients().size(), failure);
            });
            execution = null;
        } catch (IOException | InterruptedException problem) {
            if (problem instanceof InterruptedException) Thread.currentThread().interrupt();
            if (openedSecrets != null) openedSecrets.close();
            if (execution != null) execution.closeSecrets();
            failBeforeStart(offer.fence(), sender, started,
                    audience == null ? 0 : audience.recipients().size(), acknowledged, problem);
        } catch (RuntimeException problem) {
            if (openedSecrets != null) openedSecrets.close();
            if (execution != null) execution.closeSecrets();
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

    private static String serviceBaseUrl(String url) {
        URI value = URI.create(url);
        return value.getScheme() + "://" + value.getRawAuthority();
    }

    private void failBeforeStart(LeaseFence fence, AgentFrameSender sender, Instant started,
                                 long total, boolean acknowledged, Throwable failure) {
        if (acknowledged) {
            Instant ended = Instant.now();
            RemoteRunDocuments.Summary summary = new RemoteRunDocuments.Summary(fence.runId(), "FAILED",
                    total, 0, 0, 0, total, 0, 0, started, ended);
            queueCompletion(fence, encode(summary), List.of());
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
        private final SecretEnvelopeCodec.OpenedSecrets secrets;
        private final Map<String, SecretEnvelopeCodec.SecretMaterial> secretsByIdentity;
        private final ArtifactSink artifactSink;
        private RemoteExecution(LeaseFence fence, RemoteRunDocuments.Audience audience,
                                AgentFrameSender sender,
                                SecretEnvelopeCodec.OpenedSecrets secrets,
                                String serviceBaseUrl) throws JsonProcessingException {
            this.fence = fence;
            this.sender = sender;
            this.secrets = secrets;
            Map<String, SecretEnvelopeCodec.SecretMaterial> indexed = new LinkedHashMap<>();
            if (secrets != null) {
                for (SecretEnvelopeCodec.SecretMaterial secret : secrets.secrets()) {
                    SecretEnvelopeCodec.SecretMaterial previous = indexed.put(identity(secret.namespace(),
                            secret.name(), secret.version()), secret);
                    if (previous != null) throw new IllegalArgumentException("secret envelope contains duplicates");
                }
            }
            this.secretsByIdentity = Map.copyOf(indexed);
            this.artifactSink = new RemoteArtifactSink(fence, serviceBaseUrl);
            List<RecipientRecord> converted = new ArrayList<>(audience.recipients().size());
            for (RemoteRunDocuments.Recipient recipient : audience.recipients()) {
                converted.add(recipient(recipient));
            }
            this.recipients = List.copyOf(converted);
        }

        private ExecutionPorts ports() {
            return new ExecutionPorts(new ListRecipientSource(recipients), ref -> {
                SecretEnvelopeCodec.SecretMaterial material = secretsByIdentity.get(
                        identity(ref.namespace(), ref.name(), ref.version()));
                if (material == null) {
                    throw new IllegalStateException("Secret is outside this run's Agent envelope");
                }
                byte[] encoded = material.copyValue();
                char[] decoded = null;
                try {
                    decoded = decode(encoded);
                    return InMemorySecretValue.of(decoded);
                } finally {
                    Arrays.fill(encoded, (byte) 0);
                    if (decoded != null) Arrays.fill(decoded, '\0');
                }
            }, new RemoteResultSink(), artifactSink, new RemoteEventSink(), ExecutionClock.system());
        }

        private synchronized void report(RemoteRunDocuments.Report report) {
            eventOutbox.append(fence, List.of(encode(report)));
            synchronized (eventPumpLock) {
                transmitPendingEvents();
            }
        }

        private void complete(RunSummary source) {
            try {
                RemoteRunDocuments.Summary summary = new RemoteRunDocuments.Summary(source.runId(),
                        source.finalState().name(), source.total(), source.succeeded(), source.failed(),
                        source.unknown(), source.unsent(), source.skipped(), source.retried(),
                        source.startedAt(), source.endedAt());
                queueCompletion(fence, encode(summary), source.artifacts().stream()
                        .map(value -> value.artifactId()).toList());
            } finally {
                closeSecrets();
            }
        }

        private void fail(Instant started, long total, Throwable failure) {
            try {
                Instant ended = Instant.now();
                RemoteRunDocuments.Summary summary = new RemoteRunDocuments.Summary(fence.runId(), "FAILED",
                        total, 0, 0, 0, total, 0, 0, started, ended);
                queueCompletion(fence, encode(summary), List.of());
                System.err.printf("Remote run %s failed: %s%n", fence.runId(), safeMessage(failure));
            } finally {
                closeSecrets();
            }
        }

        private void closeSecrets() {
            if (secrets != null) secrets.close();
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

    private static String identity(String namespace, String name, String version) {
        return namespace + '\0' + name + '\0' + version;
    }

    private static char[] decode(byte[] value) {
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(value));
            char[] chars = new char[decoded.remaining()];
            decoded.get(chars);
            if (decoded.hasArray()) Arrays.fill(decoded.array(), '\0');
            return chars;
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Secret envelope value is not valid UTF-8", exception);
        }
    }

    private final class RemoteArtifactSink implements ArtifactSink {
        private final LeaseFence fence;
        private final String serviceBaseUrl;
        private final CopyOnWriteArrayList<ArtifactRef> references = new CopyOnWriteArrayList<>();

        private RemoteArtifactSink(LeaseFence fence, String serviceBaseUrl) {
            this.fence = fence;
            this.serviceBaseUrl = serviceBaseUrl;
        }

        @Override
        public ArtifactRef write(String type, String originalName, String contentType,
                                 ContentWriter writer) throws IOException {
            Path temporary = Files.createTempFile("wepush-agent-artifact-", ".tmp");
            try {
                MessageDigest digest = sha256Digest();
                try (var output = new DigestOutputStream(Files.newOutputStream(temporary), digest)) {
                    writer.write(output);
                }
                long size = Files.size(temporary);
                String sha256 = HexFormat.of().formatHex(digest.digest());
                ArtifactUploadPlan plan = plan(type, originalName, contentType, size, sha256);
                if ("MULTIPART".equals(plan.uploadMode())) uploadMultipart(plan, temporary);
                else upload(plan, temporary);
                ArtifactCommitResult committed = commit(plan);
                if (committed.size() != size || !sha256.equals(committed.sha256())
                        || !"READY".equals(committed.state())) {
                    throw new IOException("Service committed different Artifact content");
                }
                ArtifactRef reference = new ArtifactRef(committed.artifactId(), type, sha256, size);
                references.add(reference);
                return reference;
            } catch (InterruptedException problem) {
                Thread.currentThread().interrupt();
                throw new IOException("Artifact upload was interrupted", problem);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }

        @Override
        public List<ArtifactRef> artifacts() {
            return List.copyOf(references);
        }

        private ArtifactUploadPlan plan(String type, String originalName, String contentType,
                                        long size, String sha256) throws IOException, InterruptedException {
            String path = serviceBaseUrl + "/internal/agent/v1/leases/"
                    + URLEncoder.encode(fence.leaseId(), StandardCharsets.UTF_8) + "/artifacts";
            byte[] body = mapper.writeValueAsBytes(new ArtifactPlanRequest(fence.runId(), fence.epoch(),
                    fence.fencingToken(), type, originalName, contentType, size, sha256));
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(path))
                    .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body));
            authenticate(request);
            HttpResponse<byte[]> response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 201) {
                throw new IOException("Artifact plan returned HTTP " + response.statusCode());
            }
            return mapper.readValue(response.body(), ArtifactUploadPlan.class);
        }

        private void upload(ArtifactUploadPlan plan, Path file) throws IOException, InterruptedException {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(plan.uploadUrl()))
                    .timeout(Duration.ofMinutes(30)).PUT(HttpRequest.BodyPublishers.ofFile(file));
            plan.uploadHeaders().forEach((name, value) -> {
                if (!"host".equalsIgnoreCase(name) && !"content-length".equalsIgnoreCase(name)) {
                    request.header(name, value);
                }
            });
            HttpResponse<byte[]> response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Artifact upload returned HTTP " + response.statusCode());
            }
        }

        private void uploadMultipart(ArtifactUploadPlan plan, Path file)
                throws IOException, InterruptedException {
            MultipartPlan multipart = plan.multipart();
            if (multipart == null || multipart.partCount() < 1) {
                throw new IOException("Service returned an incomplete multipart Artifact plan");
            }
            List<CompletedPart> completed = new ArrayList<>(multipart.partCount());
            try {
                int nextPart = 1;
                while (nextPart <= multipart.partCount()) {
                    int batch = Math.min(100, multipart.partCount() - nextPart + 1);
                    List<PresignedPart> parts = planParts(multipart, nextPart, batch);
                    if (parts.size() != batch) throw new IOException("Service returned incomplete part URLs");
                    for (PresignedPart part : parts) {
                        completed.add(new CompletedPart(part.partNumber(), uploadPart(file, part)));
                    }
                    nextPart += batch;
                }
                completeMultipart(multipart, completed);
            } catch (IOException | InterruptedException problem) {
                abortMultipart(multipart);
                throw problem;
            }
        }

        private String uploadPart(Path file, PresignedPart part)
                throws IOException, InterruptedException {
            return uploadFilePart(http, file, part.offset(), part.size(), URI.create(part.url()),
                    part.headers(), part.partNumber());
        }

        private List<PresignedPart> planParts(MultipartPlan multipart, int firstPart, int count)
                throws IOException, InterruptedException {
            byte[] body = mapper.writeValueAsBytes(new PartPlanRequest(
                    multipart.uploadId(), firstPart, count));
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(multipart.partPlanUrl()))
                    .timeout(Duration.ofSeconds(30)).header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body));
            authenticate(request);
            HttpResponse<byte[]> response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IOException("Artifact part plan returned HTTP " + response.statusCode());
            }
            return mapper.readValue(response.body(), new TypeReference<List<PresignedPart>>() { });
        }

        private void completeMultipart(MultipartPlan multipart, List<CompletedPart> parts)
                throws IOException, InterruptedException {
            byte[] body = mapper.writeValueAsBytes(new MultipartCompleteRequest(multipart.uploadId(), parts));
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(multipart.completeUrl()))
                    .timeout(Duration.ofMinutes(5)).header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body));
            authenticate(request);
            HttpResponse<byte[]> response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IOException("Artifact multipart completion returned HTTP " + response.statusCode());
            }
        }

        private void abortMultipart(MultipartPlan multipart) {
            try {
                String separator = multipart.abortUrl().contains("?") ? "&" : "?";
                URI uri = URI.create(multipart.abortUrl() + separator + "upload_id="
                        + URLEncoder.encode(multipart.uploadId(), StandardCharsets.UTF_8));
                HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).DELETE();
                authenticate(request);
                http.send(request.build(), HttpResponse.BodyHandlers.discarding());
            } catch (IOException ignored) {
                // Retention cleanup and S3 lifecycle policies remain the final orphan-upload fallback.
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }

        private ArtifactCommitResult commit(ArtifactUploadPlan plan)
                throws IOException, InterruptedException {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(plan.commitUrl()))
                    .timeout(Duration.ofSeconds(30)).header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody());
            authenticate(request);
            HttpResponse<byte[]> response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IOException("Artifact commit returned HTTP " + response.statusCode());
            }
            return mapper.readValue(response.body(), ArtifactCommitResult.class);
        }

        private void authenticate(HttpRequest.Builder request) {
            if (!token.isBlank()) request.header("Authorization", "Agent " + token);
        }
    }

    static String uploadFilePart(HttpClient client, Path file, long offset, long size, URI url,
                                 Map<String, String> headers, int partNumber)
            throws IOException, InterruptedException {
            IOException lastFailure = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                HttpResponse<byte[]> response = null;
                try {
                    HttpRequest.Builder request = HttpRequest.newBuilder(url)
                            .timeout(Duration.ofMinutes(30))
                            .PUT(new FileSliceBodyPublisher(file, offset, size));
                    headers.forEach((name, value) -> {
                        if (!"host".equalsIgnoreCase(name) && !"content-length".equalsIgnoreCase(name)) {
                            request.header(name, value);
                        }
                    });
                    response = client.send(request.build(),
                            HttpResponse.BodyHandlers.ofByteArray());
                } catch (IOException problem) {
                    lastFailure = problem;
                }
                if (response != null) {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return response.headers().firstValue("ETag").orElseThrow(() ->
                                new IOException("Artifact part response does not contain an ETag"));
                    }
                    lastFailure = new IOException("Artifact part " + partNumber
                            + " returned HTTP " + response.statusCode());
                    if (!retryableUploadStatus(response.statusCode())) throw lastFailure;
                }
                if (attempt < 3) Thread.sleep(200L * attempt);
            }
            throw lastFailure == null ? new IOException("Artifact part upload failed") : lastFailure;
    }

    private static boolean retryableUploadStatus(int status) {
        return status == 408 || status == 425 || status == 429 || status >= 500;
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private record ArtifactPlanRequest(String runId, long epoch, String fencingToken, String type,
                                       String originalName, String contentType, long size, String sha256) { }
    private record ArtifactUploadPlan(String artifactId, String uploadUrl, String commitUrl,
                                      Instant expiresAt, long expectedSize, String expectedSha256,
                                      Map<String, String> uploadHeaders, String uploadMode,
                                      MultipartPlan multipart) { }
    private record MultipartPlan(String uploadId, long partSize, int partCount,
                                 String partPlanUrl, String completeUrl, String abortUrl) { }
    private record PartPlanRequest(String uploadId, int firstPartNumber, int count) { }
    private record PresignedPart(int partNumber, long offset, long size, String url,
                                 Map<String, String> headers) { }
    private record CompletedPart(int partNumber, String eTag) { }
    private record MultipartCompleteRequest(String uploadId, List<CompletedPart> parts) { }
    private record ArtifactCommitResult(String artifactId, long size, String sha256,
                                        String state, Instant readyAt) { }

    private static final class FileSliceBodyPublisher implements HttpRequest.BodyPublisher {
        private final HttpRequest.BodyPublisher delegate;
        private final long length;

        private FileSliceBodyPublisher(Path file, long offset, long length) {
            if (offset < 0 || length < 1) throw new IllegalArgumentException("Artifact part range is invalid");
            this.length = length;
            this.delegate = HttpRequest.BodyPublishers.ofInputStream(() -> {
                try {
                    return new BoundedFileInputStream(file, offset, length);
                } catch (IOException problem) {
                    throw new UncheckedIOException(problem);
                }
            });
        }

        @Override
        public long contentLength() { return length; }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            delegate.subscribe(subscriber);
        }
    }

    private static final class BoundedFileInputStream extends InputStream {
        private final FileChannel channel;
        private long remaining;

        private BoundedFileInputStream(Path file, long offset, long length) throws IOException {
            this.channel = FileChannel.open(file, StandardOpenOption.READ);
            this.channel.position(offset);
            this.remaining = length;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int read = read(one, 0, 1);
            return read < 0 ? -1 : one[0] & 0xff;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (remaining == 0) return -1;
            int maximum = (int) Math.min(length, remaining);
            int read = channel.read(ByteBuffer.wrap(bytes, offset, maximum));
            if (read > 0) remaining -= read;
            return read;
        }

        @Override
        public void close() throws IOException { channel.close(); }
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
