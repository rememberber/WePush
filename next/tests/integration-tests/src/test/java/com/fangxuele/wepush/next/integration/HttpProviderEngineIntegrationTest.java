package com.fangxuele.wepush.next.integration;

import com.fangxuele.wepush.next.core.api.ArtifactSink;
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
import com.fangxuele.wepush.next.core.api.RunEvent;
import com.fangxuele.wepush.next.core.api.RunEventSink;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.core.api.RunState;
import com.fangxuele.wepush.next.core.api.RunSummary;
import com.fangxuele.wepush.next.core.engine.DefaultExecutionEngine;
import com.fangxuele.wepush.next.provider.http.HttpProviderFactory;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpProviderEngineIntegrationTest {
    @Test
    void executesACompleteHttpRunThroughProviderSpi() throws Exception {
        AtomicInteger received = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/notify", exchange -> {
            exchange.getRequestBody().readAllBytes();
            received.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        try {
            ConfigDocument account = json("account", """
                    {"baseUrl":"http://127.0.0.1:%d","allowPrivateAddresses":true}
                    """.formatted(server.getAddress().getPort()));
            ConfigDocument message = json("message", """
                    {
                      "method":"POST",
                      "path":"/notify",
                      "headers":{"Content-Type":"application/json"},
                      "bodyTemplate":"{\\\"address\\\":\\\"{{address}}\\\"}",
                      "successStatuses":[204]
                    }
                    """);
            List<RecipientRecord> recipients = List.of(
                    recipient("item-1", 0, "a@example.com"),
                    recipient("item-2", 1, "b@example.com"),
                    recipient("item-3", 2, "c@example.com"));
            List<ItemResult> results = new CopyOnWriteArrayList<>();
            List<RunEvent> events = new CopyOnWriteArrayList<>();

            try (DefaultExecutionEngine engine = new DefaultExecutionEngine(
                    List.of(new HttpProviderFactory()))) {
                RunSummary summary = engine.start(
                                new RunExecutionSpec(
                                        "integration-run",
                                        new ProviderRef(HttpProviderFactory.PROVIDER_ID, HttpProviderFactory.VERSION),
                                        account,
                                        message,
                                        ExecutionPolicies.defaults(),
                                        Map.of(),
                                        false,
                                        Instant.now()),
                                ports(recipients, results, events))
                        .completion().toCompletableFuture().get(10, TimeUnit.SECONDS);

                assertEquals(RunState.SUCCEEDED, summary.finalState());
                assertEquals(3, summary.succeeded());
                assertEquals(3, results.size());
                assertEquals(3, received.get());
                assertEquals(RunEvent.Type.RUN_COMPLETED, events.getLast().type());
            }
        } finally {
            server.stop(0);
        }
    }

    private static ExecutionPorts ports(
            List<RecipientRecord> recipients,
            List<ItemResult> results,
            List<RunEvent> events
    ) {
        AtomicInteger cursor = new AtomicInteger();
        RecipientSource source = new RecipientSource() {
            @Override
            public long totalCount() {
                return recipients.size();
            }

            @Override
            public List<RecipientRecord> nextBatch(int maximumSize) {
                int start = cursor.getAndSet(recipients.size());
                return start == 0 ? recipients : List.of();
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

    private static RecipientRecord recipient(String id, long sequence, String address) {
        return new RecipientRecord(
                id, sequence, Map.of("address", new RecipientValue.TextValue(address)));
    }

    private static ConfigDocument json(String schema, String value) {
        return new ConfigDocument(schema, "1", value.getBytes(StandardCharsets.UTF_8));
    }
}
