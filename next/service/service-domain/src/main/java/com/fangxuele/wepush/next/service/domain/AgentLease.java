package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record AgentLease(
        String id,
        WorkspaceId workspaceId,
        String runId,
        String agentId,
        String agentSessionId,
        long epoch,
        String fencingToken,
        Status status,
        Instant assignedAt,
        Instant expiresAt,
        Instant acknowledgedAt,
        Instant completedAt,
        long lastEventSequence,
        long version
) {
    public AgentLease {
        id = DomainChecks.text(id, "lease id");
        runId = DomainChecks.text(runId, "lease run id");
        agentId = DomainChecks.text(agentId, "lease agent id");
        agentSessionId = DomainChecks.text(agentSessionId, "lease agent session id");
        fencingToken = DomainChecks.text(fencingToken, "lease fencing token");
        if (workspaceId == null || epoch < 1 || status == null || assignedAt == null
                || expiresAt == null || !expiresAt.isAfter(assignedAt)
                || lastEventSequence < 0 || version < 0) {
            throw new IllegalArgumentException("agent lease is incomplete");
        }
    }

    public enum Status {
        OFFERED,
        ACKNOWLEDGED,
        RUNNING,
        COMPLETED,
        LOST,
        EXPIRED;

        public boolean active() {
            return this == OFFERED || this == ACKNOWLEDGED || this == RUNNING;
        }
    }
}
