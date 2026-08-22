package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.Optional;

public interface RunCommandRepository {
    Optional<RunCommandRecord> findById(WorkspaceId workspaceId, String runId, String commandId);

    boolean create(RunCommandRecord command);

    void acknowledge(WorkspaceId workspaceId, String runId, String commandId,
                     RunCommandRecord.Status status, String resultCode, String resultMessage,
                     Instant acknowledgedAt);
}
