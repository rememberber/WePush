package com.fangxuele.wepush.next.service.domain;

public enum RunStatus {
    PENDING,
    LEASED,
    RUNNING,
    PAUSED,
    CANCELLING,
    CANCELLED,
    SUCCEEDED,
    PARTIAL,
    FAILED,
    LOST,
    RECOVERING;

    public boolean terminal() {
        return this == CANCELLED || this == SUCCEEDED || this == PARTIAL || this == FAILED;
    }
}
