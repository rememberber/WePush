package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record RunDefinition(
        String id,
        WorkspaceId workspaceId,
        String jobId,
        RunStatus status,
        String stateReason,
        boolean dryRun,
        long total,
        long succeeded,
        long failed,
        long unknown,
        long unsent,
        long skipped,
        long retried,
        Instant createdAt,
        Instant startedAt,
        Instant endedAt,
        Instant updatedAt,
        long version
) {
    public RunDefinition {
        id = DomainChecks.text(id, "run id");
        jobId = DomainChecks.text(jobId, "run job id");
        stateReason = stateReason == null ? "" : stateReason;
        if (workspaceId == null || status == null || createdAt == null || updatedAt == null
                || updatedAt.isBefore(createdAt) || (startedAt != null && startedAt.isBefore(createdAt))
                || (endedAt != null && startedAt != null && endedAt.isBefore(startedAt))) {
            throw new IllegalArgumentException("run definition is incomplete");
        }
        DomainChecks.nonNegative(total, "run total");
        DomainChecks.nonNegative(succeeded, "run succeeded");
        DomainChecks.nonNegative(failed, "run failed");
        DomainChecks.nonNegative(unknown, "run unknown");
        DomainChecks.nonNegative(unsent, "run unsent");
        DomainChecks.nonNegative(skipped, "run skipped");
        DomainChecks.nonNegative(retried, "run retried");
        DomainChecks.nonNegative(version, "run version");
        if (status.terminal() && total != succeeded + failed + unknown + unsent + skipped) {
            throw new IllegalArgumentException("terminal run counters do not satisfy total invariant");
        }
    }
}
