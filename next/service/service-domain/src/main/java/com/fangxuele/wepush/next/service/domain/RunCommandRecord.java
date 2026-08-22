package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record RunCommandRecord(
        String id,
        WorkspaceId workspaceId,
        String runId,
        String type,
        JsonDocument payload,
        Status status,
        String resultCode,
        String resultMessage,
        Instant createdAt,
        Instant acknowledgedAt
) {
    public RunCommandRecord {
        id = DomainChecks.text(id, "command id");
        runId = DomainChecks.text(runId, "command run id");
        type = DomainChecks.text(type, "command type");
        resultCode = resultCode == null ? "" : resultCode;
        resultMessage = resultMessage == null ? "" : resultMessage;
        if (workspaceId == null || payload == null || status == null || createdAt == null) {
            throw new IllegalArgumentException("run command is incomplete");
        }
        if (status == Status.PENDING && acknowledgedAt != null) {
            throw new IllegalArgumentException("pending command cannot be acknowledged");
        }
        if (status != Status.PENDING && acknowledgedAt == null) {
            throw new IllegalArgumentException("completed command must be acknowledged");
        }
    }

    public enum Status { PENDING, ACCEPTED, REJECTED }
}
