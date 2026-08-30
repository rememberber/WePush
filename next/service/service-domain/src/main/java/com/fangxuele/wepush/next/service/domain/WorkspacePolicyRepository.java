package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.Optional;

public interface WorkspacePolicyRepository {
    Optional<WorkspacePolicy> find(WorkspaceId workspaceId);

    void createDefault(WorkspaceId workspaceId, Instant now);

    void save(WorkspacePolicy policy, long expectedVersion);

    /** Serializes capacity decisions for a Workspace in the current transaction. */
    void lock(WorkspaceId workspaceId);

    Usage usage(WorkspaceId workspaceId);

    boolean agentAlreadyBound(WorkspaceId workspaceId, String agentId);

    void reserveRun(WorkspaceId workspaceId, String runId, int targetConcurrency, Instant createdAt);

    record Usage(long agents, long concurrentRuns, long totalConcurrency, long artifactBytes) {
    }
}
