package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.infrastructure.PostgreSQLDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class PostgresNotificationBusIntegrationTest {
    @Test
    void wakesSubscribersAcrossPostgreSqlConnections() throws Exception {
        String url = System.getenv("WEPUSH_TEST_POSTGRES_URL");
        assumeTrue(url != null && !url.isBlank(), "WEPUSH_TEST_POSTGRES_URL is not configured");
        String username = environment("WEPUSH_TEST_POSTGRES_USERNAME", "wepush");
        String password = environment("WEPUSH_TEST_POSTGRES_PASSWORD", "wepush-test");

        try (var dataSource = PostgreSQLDatabase.create(url, username, password, 4);
             var bus = new PostgresNotificationBus(
                     dataSource, new JdbcTemplate(dataSource), "postgresql")) {
            CountDownLatch runPending = new CountDownLatch(1);
            CountDownLatch agentOutbox = new CountDownLatch(1);
            bus.subscribe(PostgresNotificationBus.RUN_PENDING, runPending::countDown);
            bus.subscribe(PostgresNotificationBus.AGENT_OUTBOX, agentOutbox::countDown);
            bus.start();

            for (int attempt = 0; attempt < 20
                    && (runPending.getCount() > 0 || agentOutbox.getCount() > 0); attempt++) {
                bus.publish(PostgresNotificationBus.RUN_PENDING, "run-integration");
                bus.publish(PostgresNotificationBus.AGENT_OUTBOX, "agent-integration");
                runPending.await(100, TimeUnit.MILLISECONDS);
                agentOutbox.await(100, TimeUnit.MILLISECONDS);
            }

            assertTrue(runPending.await(1, TimeUnit.SECONDS), "run notification was not delivered");
            assertTrue(agentOutbox.await(1, TimeUnit.SECONDS), "agent notification was not delivered");
        }
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
