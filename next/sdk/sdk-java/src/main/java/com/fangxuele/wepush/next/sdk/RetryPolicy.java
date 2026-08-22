package com.fangxuele.wepush.next.sdk;

import java.time.Duration;

public record RetryPolicy(int maximumAttempts, Duration initialDelay, Duration maximumDelay) {
    public RetryPolicy {
        if (maximumAttempts < 1) {
            throw new IllegalArgumentException("maximumAttempts must be at least one");
        }
        if (initialDelay.isNegative() || maximumDelay.isNegative() || maximumDelay.compareTo(initialDelay) < 0) {
            throw new IllegalArgumentException("retry delays must be ordered non-negative durations");
        }
    }

    public static RetryPolicy defaults() {
        return new RetryPolicy(3, Duration.ofMillis(150), Duration.ofSeconds(2));
    }

    Duration delayForAttempt(int attempt) {
        long multiplier = 1L << Math.min(Math.max(attempt - 1, 0), 20);
        long millis;
        try {
            millis = Math.multiplyExact(initialDelay.toMillis(), multiplier);
        } catch (ArithmeticException ignored) {
            millis = maximumDelay.toMillis();
        }
        return Duration.ofMillis(Math.min(millis, maximumDelay.toMillis()));
    }
}
