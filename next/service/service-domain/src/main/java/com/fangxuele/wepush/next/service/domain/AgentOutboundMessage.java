package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

/** Durable Service-to-Agent protocol message. */
public record AgentOutboundMessage(
        String id,
        WorkspaceId workspaceId,
        String runId,
        String agentId,
        String leaseId,
        Type type,
        String commandType,
        JsonDocument payload,
        Instant createdAt,
        Instant nextAttemptAt,
        Instant deliveredAt,
        Instant acknowledgedAt,
        int attempts,
        String lastError
) {
    public AgentOutboundMessage {
        id = DomainChecks.text(id, "Agent outbound message id");
        runId = DomainChecks.text(runId, "Agent outbound run id");
        agentId = DomainChecks.text(agentId, "Agent outbound Agent id");
        leaseId = DomainChecks.text(leaseId, "Agent outbound Lease id");
        commandType = commandType == null ? "" : commandType;
        lastError = lastError == null ? "" : lastError;
        if (workspaceId == null || type == null || payload == null || createdAt == null
                || nextAttemptAt == null || attempts < 0) {
            throw new IllegalArgumentException("Agent outbound message is incomplete");
        }
        if (type == Type.RUN_COMMAND && commandType.isBlank()) {
            throw new IllegalArgumentException("Agent command type is required");
        }
    }

    public enum Type { LEASE_OFFER, RUN_COMMAND }
}
