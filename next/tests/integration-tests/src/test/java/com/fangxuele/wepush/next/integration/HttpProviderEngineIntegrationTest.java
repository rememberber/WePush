package com.fangxuele.wepush.next.integration;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RecipientValue;
import com.fangxuele.wepush.next.core.api.RunEvent;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.core.api.RunState;
import com.fangxuele.wepush.next.core.api.RunSummary;
import com.fangxuele.wepush.next.embedded.InMemoryExecutionStore;
import com.fangxuele.wepush.next.embedded.WePushEngine;
import com.fangxuele.wepush.next.provider.http.HttpProviderFactory;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
            InMemoryExecutionStore store = new InMemoryExecutionStore();

            try (WePushEngine engine = WePushEngine.builder()
                    .provider(new HttpProviderFactory())
                    .resultSink(store)
                    .eventSink(store)
                    .build()) {
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
                                recipients)
                        .completion().toCompletableFuture().get(10, TimeUnit.SECONDS);

                assertEquals(RunState.SUCCEEDED, summary.finalState());
                assertEquals(3, summary.succeeded());
                assertEquals(3, store.results().size());
                assertEquals(3, received.get());
                assertEquals(RunEvent.Type.RUN_COMPLETED, store.events().getLast().type());
            }
        } finally {
            server.stop(0);
        }
    }

    private static RecipientRecord recipient(String id, long sequence, String address) {
        return new RecipientRecord(
                id, sequence, Map.of("address", new RecipientValue.TextValue(address)));
    }

    private static ConfigDocument json(String schema, String value) {
        return new ConfigDocument(schema, "1", value.getBytes(StandardCharsets.UTF_8));
    }
}
