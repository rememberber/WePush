package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class RunAggregate {
    private static final Map<RunStatus, Set<RunStatus>> TRANSITIONS = transitions();

    private final WorkspaceId workspaceId;
    private final String runId;
    private final Instant createdAt;
    private RunStatus status;
    private Instant updatedAt;
    private long version;

    public RunAggregate(
            WorkspaceId workspaceId,
            String runId,
            RunStatus status,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        if (workspaceId == null || runId == null || runId.isBlank() || status == null
                || createdAt == null || updatedAt == null || updatedAt.isBefore(createdAt) || version < 0) {
            throw new IllegalArgumentException("run aggregate is incomplete");
        }
        this.workspaceId = workspaceId;
        this.runId = runId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static RunAggregate pending(WorkspaceId workspaceId, String runId, Instant createdAt) {
        return new RunAggregate(workspaceId, runId, RunStatus.PENDING, createdAt, createdAt, 0);
    }

    public void transitionTo(RunStatus target, Instant changedAt) {
        if (target == null || changedAt == null || changedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException("target and monotonic changedAt are required");
        }
        if (!TRANSITIONS.getOrDefault(status, Set.of()).contains(target)) {
            throw new IllegalStateException("Run cannot transition from " + status + " to " + target);
        }
        status = target;
        updatedAt = changedAt;
        version++;
    }

    public WorkspaceId workspaceId() {
        return workspaceId;
    }

    public String runId() {
        return runId;
    }

    public RunStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    private static Map<RunStatus, Set<RunStatus>> transitions() {
        Map<RunStatus, Set<RunStatus>> values = new EnumMap<>(RunStatus.class);
        values.put(RunStatus.PENDING, Set.of(RunStatus.LEASED, RunStatus.CANCELLED, RunStatus.FAILED));
        values.put(RunStatus.LEASED, Set.of(RunStatus.RUNNING, RunStatus.PENDING, RunStatus.LOST, RunStatus.FAILED));
        values.put(RunStatus.RUNNING, Set.of(
                RunStatus.PAUSED, RunStatus.CANCELLING, RunStatus.SUCCEEDED,
                RunStatus.PARTIAL, RunStatus.FAILED, RunStatus.LOST));
        values.put(RunStatus.PAUSED, Set.of(RunStatus.RUNNING, RunStatus.CANCELLING, RunStatus.LOST));
        values.put(RunStatus.CANCELLING, Set.of(RunStatus.CANCELLED, RunStatus.PARTIAL, RunStatus.FAILED, RunStatus.LOST));
        values.put(RunStatus.LOST, Set.of(RunStatus.RECOVERING, RunStatus.PARTIAL, RunStatus.FAILED));
        values.put(RunStatus.RECOVERING, Set.of(RunStatus.PENDING, RunStatus.RUNNING, RunStatus.PARTIAL, RunStatus.FAILED));
        return Map.copyOf(values);
    }
}
