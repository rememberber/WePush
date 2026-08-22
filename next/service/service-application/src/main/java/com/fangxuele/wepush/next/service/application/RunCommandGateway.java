package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.core.api.CommandResult;
import com.fangxuele.wepush.next.core.api.RunCommand;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;

public interface RunCommandGateway {
    CommandResult submit(WorkspaceId workspaceId, String runId, RunCommand command);
}
