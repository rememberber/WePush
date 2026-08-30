package com.fangxuele.wepush.next.service.app;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** PostgreSQL LISTEN/NOTIFY fast path. Durable scheduled polling remains the correctness fallback. */
final class PostgresNotificationBus implements AutoCloseable {
    static final String RUN_EVENT = "wepush_run_event";
    static final String RUN_PENDING = "wepush_run_pending";
    static final String AGENT_OUTBOX = "wepush_agent_outbox";
    private static final List<String> CHANNELS = List.of(RUN_EVENT, RUN_PENDING, AGENT_OUTBOX);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final Map<String, CopyOnWriteArrayList<Runnable>> subscribers = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private final ExecutorService listener = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("wepush-pg-notify-listener").factory());

    PostgresNotificationBus(DataSource dataSource, JdbcTemplate jdbc, String databaseKind) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.enabled = "postgresql".equalsIgnoreCase(databaseKind);
    }

    void start() {
        if (enabled && running.compareAndSet(false, true)) listener.submit(this::listenForever);
    }

    void subscribe(String channel, Runnable callback) {
        validate(channel);
        subscribers.computeIfAbsent(channel, ignored -> new CopyOnWriteArrayList<>()).add(callback);
    }

    void publish(String channel, String payload) {
        validate(channel);
        if (!enabled) return;
        String safe = payload == null ? "" : payload;
        if (safe.length() > 1_000) safe = safe.substring(0, 1_000);
        jdbc.queryForList("SELECT pg_notify(?, ?)", channel, safe);
    }

    private void listenForever() {
        while (running.get()) {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(true);
                try (Statement statement = connection.createStatement()) {
                    for (String channel : CHANNELS) statement.execute("LISTEN " + channel);
                }
                PGConnection postgres = connection.unwrap(PGConnection.class);
                while (running.get()) {
                    PGNotification[] messages = postgres.getNotifications(30_000);
                    if (messages == null) continue;
                    for (PGNotification message : messages) notifySubscribers(message.getName());
                }
            } catch (SQLException problem) {
                if (!running.get()) return;
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void notifySubscribers(String channel) {
        subscribers.getOrDefault(channel, new CopyOnWriteArrayList<>()).forEach(callback -> {
            try { callback.run(); } catch (RuntimeException ignored) { /* the scheduled poll remains active */ }
        });
    }

    private static void validate(String channel) {
        if (!CHANNELS.contains(channel)) throw new IllegalArgumentException("Unsupported PostgreSQL channel");
    }

    @Override
    public void close() {
        running.set(false);
        listener.shutdownNow();
    }
}
