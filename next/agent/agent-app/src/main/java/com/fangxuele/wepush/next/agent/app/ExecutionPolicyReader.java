package com.fangxuele.wepush.next.agent.app;

import com.fangxuele.wepush.next.core.api.ExecutionPolicies;

import java.time.Duration;
import java.util.Map;

final class ExecutionPolicyReader {
    private ExecutionPolicyReader() {
    }

    static ExecutionPolicies read(Map<?, ?> root) {
        ExecutionPolicies defaults = ExecutionPolicies.defaults();
        Map<?, ?> concurrency = object(root.get("concurrency"));
        Map<?, ?> rateLimit = object(root.get("rateLimit"));
        Map<?, ?> retry = object(root.get("retry"));
        Map<?, ?> timeout = object(root.get("timeout"));
        Map<?, ?> result = object(root.get("result"));
        return new ExecutionPolicies(
                new ExecutionPolicies.ConcurrencyPolicy(
                        integer(concurrency, "minimum", defaults.concurrency().minimum()),
                        integer(concurrency, "target", defaults.concurrency().target()),
                        integer(concurrency, "maximum", defaults.concurrency().maximum())),
                new ExecutionPolicies.RateLimitPolicy(
                        longValue(rateLimit, "permits", defaults.rateLimit().permits()),
                        duration(rateLimit, "period", defaults.rateLimit().period())),
                new ExecutionPolicies.RetryPolicy(
                        integer(retry, "maxAttempts", defaults.retry().maxAttempts()),
                        duration(retry, "initialDelay", defaults.retry().initialDelay()),
                        duration(retry, "maxDelay", defaults.retry().maxDelay()),
                        decimal(retry, "multiplier", defaults.retry().multiplier()),
                        decimal(retry, "jitter", defaults.retry().jitter()),
                        bool(retry, "retryTimeouts", defaults.retry().retryTimeouts())),
                new ExecutionPolicies.TimeoutPolicy(
                        duration(timeout, "itemTimeout", defaults.timeout().itemTimeout()),
                        duration(timeout, "runTimeout", defaults.timeout().runTimeout())),
                new ExecutionPolicies.ResultPolicy(
                        bool(result, "saveResponseBody", defaults.result().saveResponseBody()),
                        integer(result, "batchSize", defaults.result().batchSize())));
    }

    private static Map<?, ?> object(Object value) {
        if (value == null) return Map.of();
        if (value instanceof Map<?, ?> map) return map;
        throw new IllegalArgumentException("execution policy section must be an object");
    }

    private static int integer(Map<?, ?> values, String key, int defaultValue) {
        Object value = values.get(key);
        return value == null ? defaultValue : Math.toIntExact(number(value, key).longValue());
    }

    private static long longValue(Map<?, ?> values, String key, long defaultValue) {
        Object value = values.get(key);
        return value == null ? defaultValue : number(value, key).longValue();
    }

    private static double decimal(Map<?, ?> values, String key, double defaultValue) {
        Object value = values.get(key);
        return value == null ? defaultValue : number(value, key).doubleValue();
    }

    private static Number number(Object value, String key) {
        if (value instanceof Number number) return number;
        throw new IllegalArgumentException("execution policy " + key + " must be numeric");
    }

    private static boolean bool(Map<?, ?> values, String key, boolean defaultValue) {
        Object value = values.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean bool) return bool;
        throw new IllegalArgumentException("execution policy " + key + " must be boolean");
    }

    private static Duration duration(Map<?, ?> values, String key, Duration defaultValue) {
        Object value = values.get(key);
        if (value == null) return defaultValue;
        if (value instanceof String text) return Duration.parse(text);
        throw new IllegalArgumentException("execution policy " + key + " must be an ISO-8601 duration");
    }
}
