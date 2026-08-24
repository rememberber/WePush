package com.fangxuele.wepush.next.embedded;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.ExecutionRejectedException;
import com.fangxuele.wepush.next.core.api.ItemState;
import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.ResultSink;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.core.api.RunState;
import com.fangxuele.wepush.next.provider.spi.ConnectionTestResult;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WePushEngineTest {
    @Test
    void runsInsideTheCallingProcessAndCollectsResultsAndEvents() throws Exception {
        InMemoryExecutionStore store = new InMemoryExecutionStore();
        try (WePushEngine engine = WePushEngine.builder()
                .provider(new SuccessProvider())
                .resultSink(store)
                .eventSink(store)
                .build()) {
            var summary = engine.start(spec("embedded-1", SuccessProvider.ID), List.of(
                            new RecipientRecord("alice", 0, Map.of()),
                            new RecipientRecord("bob", 1, Map.of())))
                    .completion().toCompletableFuture().get(5, TimeUnit.SECONDS);

            assertEquals(RunState.SUCCEEDED, summary.finalState());
            assertEquals(2, summary.succeeded());
            assertEquals(2, store.results("embedded-1").size());
            assertTrue(store.results().stream().allMatch(result -> result.state() == ItemState.SUCCEEDED));
            assertEquals(com.fangxuele.wepush.next.core.api.RunEvent.Type.RUN_COMPLETED,
                    store.events("embedded-1").getLast().type());
        }
    }

    @Test
    void closesRunOwnedResourcesWhenTheRunIsRejected() {
        AtomicBoolean resultClosed = new AtomicBoolean();
        AtomicBoolean eventClosed = new AtomicBoolean();
        AtomicBoolean artifactClosed = new AtomicBoolean();
        AtomicBoolean recipientsClosed = new AtomicBoolean();

        var recipients = new com.fangxuele.wepush.next.core.api.RecipientSource() {
            @Override
            public long totalCount() {
                return 0;
            }

            @Override
            public List<RecipientRecord> nextBatch(int maximumSize) {
                return List.of();
            }

            @Override
            public void close() {
                recipientsClosed.set(true);
            }
        };

        try (WePushEngine engine = WePushEngine.builder()
                .provider(new SuccessProvider())
                .resultSinkFactory(ignored -> closeTrackingResultSink(resultClosed))
                .eventSinkFactory(ignored -> closeTrackingEventSink(eventClosed))
                .artifactSinkFactory(ignored -> closeTrackingArtifactSink(artifactClosed))
                .build()) {
            ExecutionRejectedException exception = assertThrows(ExecutionRejectedException.class,
                    () -> engine.start(spec("unknown-provider", "missing"), recipients));

            assertEquals("PROVIDER_NOT_AVAILABLE", exception.code());
            assertTrue(recipientsClosed.get());
            assertTrue(resultClosed.get());
            assertTrue(eventClosed.get());
            assertTrue(artifactClosed.get());
        }
    }

    @Test
    void requiresExplicitProviderAndResultAdapter() {
        assertThrows(IllegalStateException.class, () -> WePushEngine.builder().build());
        assertThrows(IllegalStateException.class, () -> WePushEngine.builder()
                .provider(new SuccessProvider())
                .build());
    }

    @Test
    void rejectsRunsAfterClose() {
        WePushEngine engine = WePushEngine.builder()
                .provider(new SuccessProvider())
                .resultSink(new InMemoryExecutionStore())
                .build();
        engine.close();

        ExecutionRejectedException exception = assertThrows(ExecutionRejectedException.class,
                () -> engine.start(spec("closed", SuccessProvider.ID), List.of()));
        assertEquals("EMBEDDED_ENGINE_CLOSED", exception.code());
    }

    @Test
    void closesSharedResourcesOnceWhenTheEngineCloses() throws Exception {
        AtomicInteger closed = new AtomicInteger();
        var shared = new SharedStore(closed);
        WePushEngine engine = WePushEngine.builder()
                .provider(new SuccessProvider())
                .resultSink(shared)
                .eventSink(shared)
                .artifactSink(shared)
                .build();

        engine.start(spec("shared-resource", SuccessProvider.ID), List.of())
                .completion().toCompletableFuture().get(5, TimeUnit.SECONDS);
        assertEquals(0, closed.get());

        engine.close();
        engine.close();
        assertEquals(1, closed.get());
    }

    private static ResultSink closeTrackingResultSink(AtomicBoolean closed) {
        return new ResultSink() {
            @Override
            public void append(List<com.fangxuele.wepush.next.core.api.ItemResult> batch) {
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };
    }

    private static com.fangxuele.wepush.next.core.api.RunEventSink closeTrackingEventSink(
            AtomicBoolean closed
    ) {
        return new com.fangxuele.wepush.next.core.api.RunEventSink() {
            @Override
            public void append(com.fangxuele.wepush.next.core.api.RunEvent event) {
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };
    }

    private static com.fangxuele.wepush.next.core.api.ArtifactSink closeTrackingArtifactSink(
            AtomicBoolean closed
    ) {
        return new com.fangxuele.wepush.next.core.api.ArtifactSink() {
            @Override
            public List<com.fangxuele.wepush.next.core.api.ArtifactRef> artifacts() {
                return List.of();
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };
    }

    private static RunExecutionSpec spec(String runId, String providerId) {
        ConfigDocument config = json("{}");
        return new RunExecutionSpec(
                runId,
                new ProviderRef(providerId, SuccessProvider.VERSION),
                config,
                config,
                ExecutionPolicies.defaults(),
                Map.of("source", "embedded-test"),
                false,
                Instant.now());
    }

    private static ConfigDocument json(String content) {
        return new ConfigDocument("test", "1", content.getBytes(StandardCharsets.UTF_8));
    }

    private static final class SuccessProvider implements ProviderFactory {
        private static final String ID = "test.success";
        private static final String VERSION = "1.0.0";
        private static final ConfigDocument SCHEMA = json("{}");

        @Override
        public ProviderDescriptor descriptor() {
            return new ProviderDescriptor(
                    ID,
                    "Test Success",
                    VERSION,
                    1,
                    Set.of(),
                    ProviderDescriptor.ThreadSafetyMode.THREAD_SAFE,
                    32,
                    Duration.ofSeconds(1),
                    SCHEMA,
                    SCHEMA,
                    SCHEMA);
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
                ) {
                    return ProviderResult.success("OK", request.itemId());
                }

                @Override
                public void close() {
                }
            };
        }
    }

    private static final class SharedStore implements ResultSink,
            com.fangxuele.wepush.next.core.api.RunEventSink,
            com.fangxuele.wepush.next.core.api.ArtifactSink {
        private final AtomicInteger closed;

        private SharedStore(AtomicInteger closed) {
            this.closed = closed;
        }

        @Override
        public void append(List<com.fangxuele.wepush.next.core.api.ItemResult> batch) {
        }

        @Override
        public void append(com.fangxuele.wepush.next.core.api.RunEvent event) {
        }

        @Override
        public List<com.fangxuele.wepush.next.core.api.ArtifactRef> artifacts() {
            return List.of();
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
            closed.incrementAndGet();
        }
    }
}
