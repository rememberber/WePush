package com.fangxuele.wepush.next.core.api;

import java.time.Duration;

public record ExecutionPolicies(
        ConcurrencyPolicy concurrency,
        RateLimitPolicy rateLimit,
        RetryPolicy retry,
        TimeoutPolicy timeout,
        ResultPolicy result
) {
    public ExecutionPolicies {
        if (concurrency == null || rateLimit == null || retry == null || timeout == null || result == null) {
            throw new IllegalArgumentException("execution policies must not contain null values");
        }
    }

    public static ExecutionPolicies defaults() {
        return new ExecutionPolicies(
                new ConcurrencyPolicy(1, 8, 256),
                RateLimitPolicy.unlimited(),
                new RetryPolicy(3, Duration.ofMillis(200), Duration.ofSeconds(5), 2.0, 0.2, false),
                new TimeoutPolicy(Duration.ofSeconds(30), Duration.ofHours(1)),
                new ResultPolicy(false, 200));
    }

    public record ConcurrencyPolicy(int minimum, int target, int maximum) {
        public ConcurrencyPolicy {
            if (minimum < 1 || target < minimum || maximum < target) {
                throw new IllegalArgumentException("concurrency must satisfy 1 <= minimum <= target <= maximum");
            }
        }
    }

    public record RateLimitPolicy(long permits, Duration period) {
        public RateLimitPolicy {
            if (permits < 0) {
                throw new IllegalArgumentException("permits must be non-negative");
            }
            if (period == null || period.isNegative() || period.isZero()) {
                throw new IllegalArgumentException("period must be positive");
            }
        }

        public static RateLimitPolicy unlimited() {
            return new RateLimitPolicy(0, Duration.ofSeconds(1));
        }

        public boolean limited() {
            return permits > 0;
        }
    }

    public record RetryPolicy(
            int maxAttempts,
            Duration initialDelay,
            Duration maxDelay,
            double multiplier,
            double jitter,
            boolean retryTimeouts
    ) {
        public RetryPolicy {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be at least 1");
            }
            if (initialDelay == null || initialDelay.isNegative()
                    || maxDelay == null || maxDelay.isNegative() || maxDelay.compareTo(initialDelay) < 0) {
                throw new IllegalArgumentException("retry delays are invalid");
            }
            if (multiplier < 1.0 || jitter < 0.0 || jitter > 1.0) {
                throw new IllegalArgumentException("retry multiplier or jitter is invalid");
            }
        }
    }

    public record TimeoutPolicy(Duration itemTimeout, Duration runTimeout) {
        public TimeoutPolicy {
            if (itemTimeout == null || itemTimeout.isNegative() || itemTimeout.isZero()
                    || runTimeout == null || runTimeout.isNegative() || runTimeout.isZero()) {
                throw new IllegalArgumentException("timeouts must be positive");
            }
        }
    }

    public record ResultPolicy(boolean saveResponseBody, int batchSize) {
        public ResultPolicy {
            if (batchSize < 1 || batchSize > 10_000) {
                throw new IllegalArgumentException("batchSize must be between 1 and 10000");
            }
        }
    }
}
