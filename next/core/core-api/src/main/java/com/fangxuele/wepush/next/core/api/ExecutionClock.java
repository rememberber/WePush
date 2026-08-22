package com.fangxuele.wepush.next.core.api;

import java.time.Duration;
import java.time.Instant;

public interface ExecutionClock {
    Instant now();

    void sleep(Duration duration) throws InterruptedException;

    static ExecutionClock system() {
        return new ExecutionClock() {
            @Override
            public Instant now() {
                return Instant.now();
            }

            @Override
            public void sleep(Duration duration) throws InterruptedException {
                Thread.sleep(duration);
            }
        };
    }
}
