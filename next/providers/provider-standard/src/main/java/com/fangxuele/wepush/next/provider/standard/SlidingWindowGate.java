package com.fangxuele.wepush.next.provider.standard;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

final class SlidingWindowGate {
    private final Window first;
    private final Window second;

    SlidingWindowGate(int firstLimit, Duration firstDuration, int secondLimit, Duration secondDuration) {
        first = new Window(firstLimit, firstDuration, new ArrayDeque<>());
        second = new Window(secondLimit, secondDuration, new ArrayDeque<>());
    }

    synchronized Duration acquire(Instant now) {
        prune(first, now);
        prune(second, now);
        Duration wait = maximum(waitFor(first, now), waitFor(second, now));
        if (!wait.isZero()) return wait;
        if (first.enabled()) first.timestamps.addLast(now);
        if (second.enabled()) second.timestamps.addLast(now);
        return Duration.ZERO;
    }

    private static void prune(Window window, Instant now) {
        if (!window.enabled()) return;
        Instant threshold = now.minus(window.duration);
        while (!window.timestamps.isEmpty() && !window.timestamps.peekFirst().isAfter(threshold)) {
            window.timestamps.removeFirst();
        }
    }

    private static Duration waitFor(Window window, Instant now) {
        if (!window.enabled() || window.timestamps.size() < window.limit) return Duration.ZERO;
        Duration wait = Duration.between(now, window.timestamps.peekFirst().plus(window.duration));
        return wait.isNegative() ? Duration.ZERO : wait;
    }

    private static Duration maximum(Duration left, Duration right) {
        return left.compareTo(right) >= 0 ? left : right;
    }

    private record Window(int limit, Duration duration, Deque<Instant> timestamps) {
        boolean enabled() { return limit > 0 && duration != null && !duration.isZero(); }
    }
}
