package com.fangxuele.wepush.next.embedded;

import com.fangxuele.wepush.next.core.api.ItemResult;
import com.fangxuele.wepush.next.core.api.ResultSink;
import com.fangxuele.wepush.next.core.api.RunEvent;
import com.fangxuele.wepush.next.core.api.RunEventSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Thread-safe, unbounded in-memory Result and Event adapter for tests and small embedded Runs.
 * Long-running applications should provide bounded or persistent adapters instead.
 */
public final class InMemoryExecutionStore implements ResultSink, RunEventSink {
    private final Object monitor = new Object();
    private final List<ItemResult> results = new ArrayList<>();
    private final List<RunEvent> events = new ArrayList<>();

    @Override
    public void append(List<ItemResult> batch) {
        List<ItemResult> snapshot = List.copyOf(Objects.requireNonNull(batch, "batch"));
        synchronized (monitor) {
            results.addAll(snapshot);
        }
    }

    @Override
    public void append(RunEvent event) {
        synchronized (monitor) {
            events.add(Objects.requireNonNull(event, "event"));
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    public List<ItemResult> results() {
        synchronized (monitor) {
            return List.copyOf(results);
        }
    }

    public List<ItemResult> results(String runId) {
        Objects.requireNonNull(runId, "runId");
        synchronized (monitor) {
            return results.stream().filter(result -> runId.equals(result.runId())).toList();
        }
    }

    public List<RunEvent> events() {
        synchronized (monitor) {
            return List.copyOf(events);
        }
    }

    public List<RunEvent> events(String runId) {
        Objects.requireNonNull(runId, "runId");
        synchronized (monitor) {
            return events.stream().filter(event -> runId.equals(event.runId())).toList();
        }
    }

    public void clear() {
        synchronized (monitor) {
            results.clear();
            events.clear();
        }
    }
}
