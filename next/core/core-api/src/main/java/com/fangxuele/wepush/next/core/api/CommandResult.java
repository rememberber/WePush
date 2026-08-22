package com.fangxuele.wepush.next.core.api;

public record CommandResult(String commandId, Status status, String code, String message) {
    public CommandResult {
        commandId = ApiChecks.notBlank(commandId, "commandId");
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        code = ApiChecks.notBlank(code, "code");
        message = message == null ? "" : message;
    }

    public static CommandResult accepted(String commandId, String code) {
        return new CommandResult(commandId, Status.ACCEPTED, code, "");
    }

    public static CommandResult rejected(String commandId, String code, String message) {
        return new CommandResult(commandId, Status.REJECTED, code, message);
    }

    public enum Status {
        ACCEPTED,
        REJECTED
    }
}
