package com.fangxuele.tool.push.logic.msgsender;

import com.fangxuele.tool.push.App;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * 按平台和账号共享的并发闸门，避免同时运行多个任务时把各自线程数叠加到第三方 API。
 */
public final class ProviderTrafficController {
    private static final Map<Key, Gate> LIMITERS = new ConcurrentHashMap<>();

    private ProviderTrafficController() {
    }

    public static IMsgSender limit(IMsgSender delegate, int messageType, int accountId) {
        int permits = configuredPermits(messageType);
        Gate gate = LIMITERS.computeIfAbsent(new Key(messageType, accountId),
                ignored -> new Gate(new AdjustableSemaphore(permits), new AtomicInteger(permits), new AtomicLong()));
        gate.updatePermits(permits);
        return new LimitedMsgSender(delegate, gate);
    }

    public static void invalidate(int messageType, int accountId) {
        LIMITERS.keySet().removeIf(key -> key.messageType() == messageType && key.accountId() == accountId);
    }

    public static void invalidateAccount(int accountId) {
        LIMITERS.keySet().removeIf(key -> key.accountId() == accountId);
    }

    public static void resetAll() {
        LIMITERS.clear();
    }

    private static int configuredPermits(int messageType) {
        String override = System.getProperty("wepush.http.maxConcurrency." + messageType);
        if (override != null) {
            try {
                return Math.max(1, Integer.parseInt(override.trim()));
            } catch (NumberFormatException ignored) {
                // 使用全局设置。
            }
        }
        try {
            return Math.max(1, App.config.getMaxThreads());
        } catch (Exception ignored) {
            return 100;
        }
    }

    private record Key(int messageType, int accountId) {
    }

    private record Gate(AdjustableSemaphore semaphore,
                        AtomicInteger configuredPermits,
                        AtomicLong blockedUntilNanos) {
        synchronized void updatePermits(int permits) {
            int previous = configuredPermits.getAndSet(permits);
            int difference = permits - previous;
            if (difference > 0) {
                semaphore.release(difference);
            } else if (difference < 0) {
                semaphore.reduce(-difference);
            }
        }
    }

    private static final class AdjustableSemaphore extends Semaphore {
        private AdjustableSemaphore(int permits) {
            super(permits, true);
        }

        private void reduce(int permits) {
            reducePermits(permits);
        }
    }

    private static final class LimitedMsgSender implements IMsgSender {
        private final IMsgSender delegate;
        private final Gate gate;

        private LimitedMsgSender(IMsgSender delegate, Gate gate) {
            this.delegate = delegate;
            this.gate = gate;
        }

        @Override
        public SendResult send(String[] msgData) {
            boolean acquired = false;
            try {
                awaitThrottleWindow();
                gate.semaphore().acquire();
                acquired = true;
                SendResult result = delegate.send(msgData);
                updateThrottleWindow(result);
                return result;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                SendResult result = new SendResult();
                result.setSuccess(false);
                result.setInfo("等待平台并发配额时任务被中断");
                return result;
            } finally {
                if (acquired) {
                    gate.semaphore().release();
                }
            }
        }

        @Override
        public SendResult asyncSend(String[] msgData) {
            return send(msgData);
        }

        @Override
        public int recommendedBatchSize() {
            return delegate.recommendedBatchSize();
        }

        @Override
        public List<SendResult> sendBatch(List<String[]> batch) {
            boolean acquired = false;
            try {
                awaitThrottleWindow();
                gate.semaphore().acquire();
                acquired = true;
                List<SendResult> results = delegate.sendBatch(batch);
                results.forEach(this::updateThrottleWindow);
                return results;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                List<SendResult> results = new ArrayList<>(batch.size());
                for (int i = 0; i < batch.size(); i++) {
                    SendResult result = new SendResult();
                    result.setSuccess(false);
                    result.setInfo("等待平台并发配额时任务被中断");
                    results.add(result);
                }
                return results;
            } finally {
                if (acquired) {
                    gate.semaphore().release();
                }
            }
        }

        private void awaitThrottleWindow() throws InterruptedException {
            while (true) {
                long remaining = gate.blockedUntilNanos().get() - System.nanoTime();
                if (remaining <= 0) {
                    return;
                }
                LockSupport.parkNanos(Math.min(remaining, TimeUnit.SECONDS.toNanos(1)));
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        }

        private void updateThrottleWindow(SendResult result) {
            if (result == null || (!Integer.valueOf(429).equals(result.getHttpStatus())
                    && result.getRetryAfterMillis() == null)) {
                return;
            }
            long delayMillis = result.getRetryAfterMillis() == null
                    ? 1_000 : Math.max(1, result.getRetryAfterMillis());
            long blockedUntil = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayMillis);
            gate.blockedUntilNanos().accumulateAndGet(blockedUntil, Math::max);
        }
    }
}
