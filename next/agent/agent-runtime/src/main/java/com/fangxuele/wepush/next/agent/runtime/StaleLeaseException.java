package com.fangxuele.wepush.next.agent.runtime;

public final class StaleLeaseException extends IllegalStateException {
    public StaleLeaseException(String message) {
        super(message);
    }
}
