package com.fangxuele.wepush.next.service.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface AccountAuthCircuitRepository {
    Optional<AccountAuthCircuit> find(WorkspaceId workspaceId, String accountId);

    Optional<String> accountForRun(WorkspaceId workspaceId, String runId);

    AccountAuthCircuit recordFailure(WorkspaceId workspaceId, String accountId, String runId,
                                     Instant now, int threshold, Duration window, Duration openDuration);

    void reset(WorkspaceId workspaceId, String accountId);
}
