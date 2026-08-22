package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.WorkspaceId;

@FunctionalInterface
public interface RunDispatcher {
    void dispatch(WorkspaceId workspaceId, String runId);
}
