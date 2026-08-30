package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record AccountAuthCircuit(WorkspaceId workspaceId, String accountId, int failureRuns,
                                 Instant firstFailureAt, Instant lastFailureAt, Instant openUntil,
                                 String lastRunId, long version) {
    public AccountAuthCircuit {
        if (workspaceId == null || accountId == null || accountId.isBlank() || failureRuns < 0
                || version < 0) throw new IllegalArgumentException("Account authentication circuit is invalid");
        lastRunId = lastRunId == null ? "" : lastRunId;
    }

    public boolean openAt(Instant now) {
        return openUntil != null && openUntil.isAfter(now);
    }
}
