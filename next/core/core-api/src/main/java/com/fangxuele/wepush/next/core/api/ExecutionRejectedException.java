package com.fangxuele.wepush.next.core.api;

public final class ExecutionRejectedException extends RuntimeException {
    private final String code;

    public ExecutionRejectedException(String code, String message) {
        super(message);
        this.code = ApiChecks.notBlank(code, "code");
    }

    public String code() {
        return code;
    }
}
