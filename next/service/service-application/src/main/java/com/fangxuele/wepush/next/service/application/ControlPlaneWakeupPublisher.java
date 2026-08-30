package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.WorkspaceId;

/** Optional low-latency hint; durable database scans remain the correctness source. */
public interface ControlPlaneWakeupPublisher {
    void runPending(WorkspaceId workspaceId, String runId);

    void agentOutbox(String agentId);
}
