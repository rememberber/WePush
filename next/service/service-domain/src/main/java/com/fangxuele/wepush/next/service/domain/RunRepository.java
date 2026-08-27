package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RunRepository {
    void create(
            RunDefinition run,
            RunSnapshot snapshot,
            RunEventRecord createdEvent,
            IdempotencyRecord idempotencyRecord
    );

    Optional<RunDefinition> findById(WorkspaceId workspaceId, String runId);

    Optional<RunSnapshot> findSnapshot(WorkspaceId workspaceId, String runId);

    List<RunDefinition> list(WorkspaceId workspaceId);

    List<RunDefinition> page(WorkspaceId workspaceId, ResourcePageQuery query);

    List<RunDefinition> active(WorkspaceId workspaceId, int limit);

    RunOverview overview(WorkspaceId workspaceId, Instant from);

    void createRetry(RunDefinition run, RunSnapshot snapshot, RunEventRecord createdEvent,
                     IdempotencyRecord idempotencyRecord, String sourceRunId,
                     Set<String> retryStates);

    Optional<IdempotencyRecord> findIdempotency(WorkspaceId workspaceId, String scope, String keyHash);

    boolean transition(
            WorkspaceId workspaceId,
            String runId,
            Set<RunStatus> expected,
            RunStatus target,
            String reason,
            Instant changedAt
    );

    void complete(
            WorkspaceId workspaceId,
            String runId,
            RunStatus target,
            String reason,
            long total,
            long succeeded,
            long failed,
            long unknown,
            long unsent,
            long skipped,
            long retried,
            Instant endedAt
    );

    void appendEvent(RunEventRecord event);

    long nextEventSequence(WorkspaceId workspaceId, String runId);

    List<RunEventRecord> eventsAfter(WorkspaceId workspaceId, String runId, long sequenceExclusive, int limit);
}
