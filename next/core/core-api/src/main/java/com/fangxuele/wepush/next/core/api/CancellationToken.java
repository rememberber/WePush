package com.fangxuele.wepush.next.core.api;

@FunctionalInterface
public interface CancellationToken {
    boolean cancelled();

    default void throwIfCancelled() throws InterruptedException {
        if (cancelled()) {
            throw new InterruptedException("operation cancelled");
        }
    }
}
