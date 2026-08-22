package com.fangxuele.wepush.next.core.api;

import java.time.Instant;
import java.util.List;

public record RunSummary(
        String runId,
        RunState finalState,
        long total,
        long succeeded,
        long failed,
        long unknown,
        long unsent,
        long skipped,
        long retried,
        Instant startedAt,
        Instant endedAt,
        List<ArtifactRef> artifacts,
        List<ExecutionError> suppressedErrors
) {
    public RunSummary {
        runId = ApiChecks.notBlank(runId, "runId");
        if (finalState == null || !finalState.terminal()) {
            throw new IllegalArgumentException("finalState must be terminal");
        }
        if (total < 0 || succeeded < 0 || failed < 0 || unknown < 0 || unsent < 0
                || skipped < 0 || retried < 0) {
            throw new IllegalArgumentException("summary counters must be non-negative");
        }
        if (total != succeeded + failed + unknown + unsent + skipped) {
            throw new IllegalArgumentException("summary counters do not satisfy total invariant");
        }
        if (startedAt == null || endedAt == null || endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("summary timestamps are invalid");
        }
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
        suppressedErrors = suppressedErrors == null ? List.of() : List.copyOf(suppressedErrors);
    }
}
