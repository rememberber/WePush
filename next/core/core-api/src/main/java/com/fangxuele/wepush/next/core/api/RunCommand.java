package com.fangxuele.wepush.next.core.api;

public sealed interface RunCommand permits RunCommand.PauseRun, RunCommand.ResumeRun,
        RunCommand.CancelRun, RunCommand.ChangeConcurrency {
    String commandId();

    record PauseRun(String commandId) implements RunCommand {
        public PauseRun {
            commandId = ApiChecks.notBlank(commandId, "commandId");
        }
    }

    record ResumeRun(String commandId) implements RunCommand {
        public ResumeRun {
            commandId = ApiChecks.notBlank(commandId, "commandId");
        }
    }

    record CancelRun(String commandId, String reason) implements RunCommand {
        public CancelRun {
            commandId = ApiChecks.notBlank(commandId, "commandId");
            reason = ApiChecks.notBlank(reason, "reason");
        }
    }

    record ChangeConcurrency(String commandId, int target) implements RunCommand {
        public ChangeConcurrency {
            commandId = ApiChecks.notBlank(commandId, "commandId");
            if (target < 1) {
                throw new IllegalArgumentException("target must be positive");
            }
        }
    }
}
