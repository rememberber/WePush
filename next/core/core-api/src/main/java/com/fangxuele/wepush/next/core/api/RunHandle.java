package com.fangxuele.wepush.next.core.api;

import java.util.concurrent.CompletionStage;

public interface RunHandle {
    String runId();

    RunState state();

    CommandResult submit(RunCommand command);

    CompletionStage<RunSummary> completion();
}
