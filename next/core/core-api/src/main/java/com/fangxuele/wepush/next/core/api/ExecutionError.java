package com.fangxuele.wepush.next.core.api;

public record ExecutionError(String code, String message, String phase) {
    public ExecutionError {
        code = ApiChecks.notBlank(code, "code");
        message = ApiChecks.notBlank(message, "message");
        phase = ApiChecks.notBlank(phase, "phase");
    }
}
