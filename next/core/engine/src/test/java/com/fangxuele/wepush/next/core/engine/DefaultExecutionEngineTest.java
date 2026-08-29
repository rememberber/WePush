package com.fangxuele.wepush.next.core.engine;

import com.fangxuele.wepush.next.core.api.ArtifactSink;
import com.fangxuele.wepush.next.core.api.CommandResult;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.ExecutionPorts;
import com.fangxuele.wepush.next.core.api.ItemResult;
import com.fangxuele.wepush.next.core.api.ItemState;
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
import com.fangxuele.wepush.next.core.api.RunState;
import com.fangxuele.wepush.next.core.api.RunSummary;
import com.fangxuele.wepush.next.provider.spi.ConnectionTestResult;
import com.fangxuele.wepush.next.provider.spi.ErrorCategory;
import com.fangxuele.wepush.next.provider.spi.ProviderDescriptor;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.provider.spi.ProviderOpenContext;
import com.fangxuele.wepush.next.provider.spi.ProviderResult;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import com.fangxuele.wepush.next.provider.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultExecutionEngineTest {
    @Test
    void streamsRecipientsRetriesTransientFailuresAndPreservesSummaryInvariant() throws Exception {
        Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
        TestProvider provider = new TestProvider((request, token) -> {
            int attempt = attempts.computeIfAbsent(request.itemId(), ignored -> new AtomicInteger())
                    .incrementAndGet();
            if (request.itemId().equals("item-0") && attempt == 1) {
                return ProviderResult.failure(
                        "TEMPORARY", ErrorCategory.TEMPORARY_REMOTE, true, "temporary failure");
            }
            if (request.itemId().equals("item-1")) {
                return ProviderResult.failure(
                        "REJECTED", ErrorCategory.PERMANENT_REMOTE, false, "permanent failure");
            }
            return ProviderResult.success("OK", "request-" + request.itemId());
        });
        TestPorts ports = new TestPorts(recipients(4));

        try (DefaultExecutionEngine engine = new DefaultExecutionEngine(List.of(provider))) {
            RunSummary summary = engine.start(spec("run-retry", policies(2, 3)), ports.executionPorts())
                    .completion().toCompletableFuture().get(5, TimeUnit.SECONDS);

            assertEquals(RunState.PARTIAL, summary.finalState(), summary::toString);
            assertEquals(4, summary.total());
            assertEquals(3, summary.succeeded());
            assertEquals(1, summary.failed());
            assertEquals(0, summary.unknown());
            assertEquals(0, summary.unsent());
            assertEquals(1, summary.retried());
            assertEquals(summary.total(), summary.succeeded() + summary.failed()
                    + summary.unknown() + summary.unsent() + summary.skipped());
            assertEquals(4, ports.results.size());
            assertEquals(RunEvent.Type.RUN_STARTED, ports.events.getFirst().type());
            assertEquals(RunEvent.Type.RUN_COMPLETED, ports.events.getLast().type());
        }
    }

    @Test
    void commandsAreIdempotentAndCancellationLeavesUnstartedRecipientsUnsent() throws Exception {
        CountDownLatch enteredProvider = new CountDownLatch(1);
        TestProvider provider = new TestProvider((request, token) -> {
            enteredProvider.countDown();
            while (!token.cancelled()) {
                Thread.sleep(Duration.ofMillis(5));
            }
            return new ProviderResult(
                    ItemState.UNKNOWN, "CANCELLED_IN_FLIGHT", ErrorCategory.CANCELLED,
                    false, null, "unknown after cancellation", "", Map.of());
        });
        TestPorts ports = new TestPorts(recipients(10));

        try (DefaultExecutionEngine engine = new DefaultExecutionEngine(List.of(provider))) {
            RunHandle handle = engine.start(spec("run-cancel", policies(1, 1)), ports.executionPorts());
            assertTrue(enteredProvider.await(2, TimeUnit.SECONDS));

            CommandResult first = handle.submit(new RunCommand.CancelRun("cancel-1", "test"));
            CommandResult repeated = handle.submit(new RunCommand.CancelRun("cancel-1", "ignored"));
            RunSummary summary = handle.completion().toCompletableFuture().get(5, TimeUnit.SECONDS);

            assertSame(first, repeated);
            assertEquals(CommandResult.Status.ACCEPTED, first.status());
            assertEquals(RunState.CANCELLED, summary.finalState());
            assertEquals(10, summary.total());
            assertTrue(summary.unknown() >= 1);
            assertTrue(summary.unsent() >= 1);
            assertEquals(summary.total(), summary.succeeded() + summary.failed()
                    + summary.unknown() + summary.unsent() + summary.skipped());
        }
    }

    @Test
    void rejectsConcurrencyOutsideProviderLimit() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        TestProvider provider = new TestProvider((request, token) -> {
            release.await(2, TimeUnit.SECONDS);
            return ProviderResult.success("OK", "request");
        });
        TestPorts ports = new TestPorts(recipients(1));

        try (DefaultExecutionEngine engine = new DefaultExecutionEngine(List.of(provider))) {
            RunHandle handle = engine.start(spec("run-concurrency", policies(1, 1)), ports.executionPorts());
            awaitState(handle, RunState.RUNNING);

            CommandResult result = handle.submit(new RunCommand.ChangeConcurrency("change-1", 9));
            assertEquals(CommandResult.Status.REJECTED, result.status());
            assertEquals("CONCURRENCY_OUT_OF_RANGE", result.code());

            release.countDown();
            assertEquals(RunState.SUCCEEDED,
                    handle.completion().toCompletableFuture().get(5, TimeUnit.SECONDS).finalState());
        }
    }

    @Test
    void pauseStopsNewProviderCallsUntilResume() throws Exception {
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        TestProvider provider = new TestProvider((request, token) -> {
            int call = calls.incrementAndGet();
            if (call == 1) {
                firstEntered.countDown();
                releaseFirst.await(2, TimeUnit.SECONDS);
            }
            return ProviderResult.success("OK", "request");
        });
        TestPorts ports = new TestPorts(recipients(3));

        try (DefaultExecutionEngine engine = new DefaultExecutionEngine(List.of(provider))) {
            RunHandle handle = engine.start(spec("run-pause", policies(1, 1)), ports.executionPorts());
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            assertEquals(CommandResult.Status.ACCEPTED,
                    handle.submit(new RunCommand.PauseRun("pause-1")).status());

            releaseFirst.countDown();
            Thread.sleep(Duration.ofMillis(100));
            assertEquals(1, calls.get());

            assertEquals(CommandResult.Status.ACCEPTED,
                    handle.submit(new RunCommand.ResumeRun("resume-1")).status());
            RunSummary summary = handle.completion().toCompletableFuture().get(5, TimeUnit.SECONDS);
            assertEquals(RunState.SUCCEEDED, summary.finalState());
            assertEquals(3, calls.get());
        }
    }

    @Test
    void streamsOneHundredThousandRecipientsWithoutMaterializingTheAudienceOrResults() throws Exception {
        int recipientCount = 100_000;
        int batchSize = 256;
        AtomicInteger cursor = new AtomicInteger();
        AtomicInteger largestBatch = new AtomicInteger();
        AtomicLong persistedResults = new AtomicLong();
        AtomicLong persistedEvents = new AtomicLong();
        RecipientSource source = new RecipientSource() {
            @Override
            public long totalCount() {
                return recipientCount;
            }

            @Override
            public List<RecipientRecord> nextBatch(int maximumSize) {
                int start = cursor.getAndAccumulate(maximumSize,
                        (current, increment) -> Math.min(recipientCount, current + increment));
                if (start >= recipientCount) return List.of();
                int end = Math.min(recipientCount, start + maximumSize);
                largestBatch.accumulateAndGet(end - start, Math::max);
                List<RecipientRecord> batch = new ArrayList<>(end - start);
                for (int index = start; index < end; index++) {
                    batch.add(new RecipientRecord("item-" + index, index,
                            Map.of("address", new RecipientValue.TextValue("user-" + index))));
                }
                return batch;
            }
        };
        ExecutionPorts ports = new ExecutionPorts(source, ref -> {
            throw new AssertionError("No secret expected");
        }, new ResultSink() {
            @Override
            public void append(List<ItemResult> batch) {
                persistedResults.addAndGet(batch.size());
            }

            @Override
            public void flush() {
            }
        }, ArtifactSink.none(), new RunEventSink() {
            @Override
            public void append(RunEvent event) {
                persistedEvents.incrementAndGet();
            }

            @Override
            public void flush() {
            }
        }, ExecutionClock.system());
        TestProvider provider = new TestProvider((request, token) ->
                ProviderResult.success("OK", request.itemId()));
        ExecutionPolicies streamingPolicies = new ExecutionPolicies(
                new ExecutionPolicies.ConcurrencyPolicy(1, 4, 4),
                ExecutionPolicies.RateLimitPolicy.unlimited(),
                new ExecutionPolicies.RetryPolicy(1, Duration.ZERO, Duration.ZERO, 1.0, 0.0, false),
                new ExecutionPolicies.TimeoutPolicy(Duration.ofSeconds(5), Duration.ofMinutes(2)),
                new ExecutionPolicies.ResultPolicy(false, batchSize));

        try (DefaultExecutionEngine engine = new DefaultExecutionEngine(List.of(provider))) {
            RunSummary summary = engine.start(spec("run-large-audience", streamingPolicies), ports)
                    .completion().toCompletableFuture().get(2, TimeUnit.MINUTES);

            assertEquals(RunState.SUCCEEDED, summary.finalState());
            assertEquals(recipientCount, summary.total());
            assertEquals(recipientCount, summary.succeeded());
            assertEquals(recipientCount, persistedResults.get());
            assertTrue(persistedEvents.get() >= recipientCount + 2L);
            assertTrue(largestBatch.get() <= batchSize);
        }
    }

    private static void awaitState(RunHandle handle, RunState expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (handle.state() != expected && System.nanoTime() < deadline) {
            Thread.sleep(Duration.ofMillis(5));
        }
        assertEquals(expected, handle.state());
    }

    private static RunExecutionSpec spec(String runId, ExecutionPolicies policies) {
        ConfigDocument config = json("config", "{}");
        return new RunExecutionSpec(
                runId,
                new ProviderRef("test.provider", "1.0.0"),
                config,
                config,
                policies,
                Map.of(),
                false,
                Instant.now());
    }

    private static ExecutionPolicies policies(int concurrency, int maxAttempts) {
        return new ExecutionPolicies(
                new ExecutionPolicies.ConcurrencyPolicy(1, concurrency, 8),
                ExecutionPolicies.RateLimitPolicy.unlimited(),
                new ExecutionPolicies.RetryPolicy(
                        maxAttempts, Duration.ZERO, Duration.ZERO, 1.0, 0.0, false),
                new ExecutionPolicies.TimeoutPolicy(Duration.ofSeconds(2), Duration.ofSeconds(10)),
                new ExecutionPolicies.ResultPolicy(false, 2));
    }

    private static List<RecipientRecord> recipients(int count) {
        List<RecipientRecord> records = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            records.add(new RecipientRecord(
                    "item-" + index,
                    index,
                    Map.of("address", new RecipientValue.TextValue("user-" + index))));
        }
        return records;
    }

    private static ConfigDocument json(String schemaId, String value) {
        return new ConfigDocument(schemaId, "1", value.getBytes(StandardCharsets.UTF_8));
    }

    @FunctionalInterface
    private interface SendBehavior {
        ProviderResult send(
                com.fangxuele.wepush.next.provider.spi.ProviderSendRequest request,
                com.fangxuele.wepush.next.core.api.CancellationToken token
        ) throws Exception;
    }

    private static final class TestProvider implements ProviderFactory {
        private final SendBehavior behavior;
        private final ProviderDescriptor descriptor = new ProviderDescriptor(
                "test.provider",
                "Test Provider",
                "1.0.0",
                1,
                Set.of(ProviderDescriptor.Capability.IDEMPOTENCY),
                ProviderDescriptor.ThreadSafetyMode.THREAD_SAFE,
                4,
                Duration.ofSeconds(2),
                json("account-schema", "{}"),
                json("message-schema", "{}"),
                json("recipient-schema", "{}"));

        private TestProvider(SendBehavior behavior) {
            this.behavior = behavior;
        }

        @Override
        public ProviderDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public ValidationResult validateAccount(ConfigDocument account) {
            return ValidationResult.valid();
        }

        @Override
        public ValidationResult validateMessage(ConfigDocument message) {
            return ValidationResult.valid();
        }

        @Override
        public ConnectionTestResult testConnection(
                ConfigDocument account,
                com.fangxuele.wepush.next.core.api.SecretResolver secrets,
                Duration timeout
        ) {
            return new ConnectionTestResult(true, "OK", "", Duration.ZERO);
        }

        @Override
        public ProviderSession open(ProviderOpenContext context) {
            return new ProviderSession() {
                @Override
                public ProviderResult send(
                        com.fangxuele.wepush.next.provider.spi.ProviderSendRequest request,
                        com.fangxuele.wepush.next.core.api.CancellationToken token
                ) throws Exception {
                    return behavior.send(request, token);
                }

                @Override
                public void close() {
                }
            };
        }
    }

    private static final class TestPorts {
        private final List<ItemResult> results = new CopyOnWriteArrayList<>();
        private final List<RunEvent> events = new CopyOnWriteArrayList<>();
        private final List<RecipientRecord> recipients;

        private TestPorts(List<RecipientRecord> recipients) {
            this.recipients = List.copyOf(recipients);
        }

        private ExecutionPorts executionPorts() {
            AtomicInteger cursor = new AtomicInteger();
            RecipientSource source = new RecipientSource() {
                @Override
                public long totalCount() {
                    return recipients.size();
                }

                @Override
                public List<RecipientRecord> nextBatch(int maximumSize) {
                    int start = cursor.get();
                    if (start >= recipients.size()) {
                        return List.of();
                    }
                    int end = Math.min(start + maximumSize, recipients.size());
                    cursor.set(end);
                    return recipients.subList(start, end);
                }
            };
            ResultSink resultSink = new ResultSink() {
                @Override
                public void append(List<ItemResult> batch) {
                    results.addAll(batch);
                }

                @Override
                public void flush() {
                }
            };
            RunEventSink eventSink = new RunEventSink() {
                @Override
                public void append(RunEvent event) {
                    events.add(event);
                }

                @Override
                public void flush() {
                }
            };
            return new ExecutionPorts(
                    source,
                    ref -> {
                        throw new AssertionError("No secret expected");
                    },
                    resultSink,
                    ArtifactSink.none(),
                    eventSink,
                    ExecutionClock.system());
        }
    }
}
