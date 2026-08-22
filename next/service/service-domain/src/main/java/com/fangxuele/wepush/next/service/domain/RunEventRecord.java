package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record RunEventRecord(
        String runId,
        WorkspaceId workspaceId,
        long sequence,
        String type,
        Instant occurredAt,
        JsonDocument payload,
        Severity severity
) {
    public RunEventRecord {
        runId = DomainChecks.text(runId, "event run id");
        type = DomainChecks.text(type, "event type");
        if (workspaceId == null || sequence < 1 || occurredAt == null || payload == null || severity == null) {
            throw new IllegalArgumentException("run event is incomplete");
        }
    }

    public enum Severity { DEBUG, INFO, WARNING, ERROR }
}
