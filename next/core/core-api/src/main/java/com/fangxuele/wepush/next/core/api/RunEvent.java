package com.fangxuele.wepush.next.core.api;

import java.time.Instant;
import java.util.Map;

public record RunEvent(
        String runId,
        long sequence,
        Type type,
        Instant occurredAt,
        Map<String, String> data
) {
    public RunEvent {
        runId = ApiChecks.notBlank(runId, "runId");
        if (sequence < 1 || type == null || occurredAt == null) {
            throw new IllegalArgumentException("event sequence, type and time are required");
        }
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public enum Type {
        RUN_STARTED,
        STATE_CHANGED,
        CONCURRENCY_CHANGED,
        ITEM_COMPLETED,
        PROGRESS,
        RUN_COMPLETED,
        RUN_FAILED
    }
}
