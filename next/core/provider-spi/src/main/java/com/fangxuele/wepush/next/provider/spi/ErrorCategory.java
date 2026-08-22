package com.fangxuele.wepush.next.provider.spi;

public enum ErrorCategory {
    NONE,
    AUTHENTICATION,
    AUTHORIZATION,
    INVALID_REQUEST,
    RECIPIENT_INVALID,
    RATE_LIMITED,
    TEMPORARY_REMOTE,
    PERMANENT_REMOTE,
    NETWORK,
    TIMEOUT,
    CANCELLED,
    INTERNAL,
    UNKNOWN
}
