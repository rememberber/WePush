package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AgentLeaseRepository {
    Optional<AgentLease> findById(String leaseId);

    Optional<AgentLease> findCurrent(WorkspaceId workspaceId, String runId);

    List<AgentLease> activeForAgent(String agentId, String agentSessionId);

    int offeredCount(String agentId, String agentSessionId);

    long nextEpoch(WorkspaceId workspaceId, String runId);

    void create(AgentLease lease);

    boolean acknowledge(String leaseId, String agentId, String agentSessionId,
                        String fencingToken, Instant acknowledgedAt);

    boolean markRunning(String leaseId, String fencingToken);

    boolean renew(LeaseFenceAuthority authority, String agentId, String agentSessionId,
                  Instant now, Instant expiresAt);

    boolean advanceEvents(String leaseId, String fencingToken, long expectedPrevious,
                          long lastEventSequence);

    boolean complete(String leaseId, String fencingToken, Instant completedAt);

    boolean markLost(String leaseId, String fencingToken, Instant changedAt);

    List<AgentLease> expireActive(Instant now);

    record LeaseFenceAuthority(String leaseId, String runId, long epoch, String fencingToken) {
        public LeaseFenceAuthority {
            if (leaseId == null || leaseId.isBlank() || runId == null || runId.isBlank()
                    || epoch < 1 || fencingToken == null || fencingToken.isBlank()) {
                throw new IllegalArgumentException("lease fence authority is incomplete");
            }
        }
    }
}
