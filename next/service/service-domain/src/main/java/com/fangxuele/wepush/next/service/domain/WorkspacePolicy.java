package com.fangxuele.wepush.next.service.domain;

import java.time.Duration;
import java.time.Instant;

/** Workspace-scoped resource limits. A zero limit means unlimited. */
public record WorkspacePolicy(
        WorkspaceId workspaceId,
        int maxAgents,
        int maxConcurrentRuns,
        int maxTotalConcurrency,
        long artifactQuotaBytes,
        long artifactRetentionSeconds,
        Instant updatedAt,
        long version) {

    public static final long DEFAULT_RETENTION_SECONDS = Duration.ofDays(7).toSeconds();

    public WorkspacePolicy {
        if (workspaceId == null || maxAgents < 0 || maxConcurrentRuns < 0
                || maxTotalConcurrency < 0 || artifactQuotaBytes < 0
                || artifactRetentionSeconds < 300
                || artifactRetentionSeconds > Duration.ofDays(3650).toSeconds()
                || updatedAt == null || version < 0) {
            throw new IllegalArgumentException("Workspace Policy contains invalid resource limits");
        }
    }

    public Duration artifactRetention() {
        return Duration.ofSeconds(artifactRetentionSeconds);
    }
}
