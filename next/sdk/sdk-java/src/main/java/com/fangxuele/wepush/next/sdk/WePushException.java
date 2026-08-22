package com.fangxuele.wepush.next.sdk;

public final class WePushException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    WePushException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    WePushException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.responseBody = "";
    }

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }
}
