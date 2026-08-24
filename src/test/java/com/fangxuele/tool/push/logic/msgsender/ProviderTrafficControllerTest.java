package com.fangxuele.tool.push.logic.msgsender;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProviderTrafficControllerTest {
    private static final int MESSAGE_TYPE = 90_003;
    private static final int ACCOUNT_ID = 90_004;

    @After
    public void tearDown() {
        System.clearProperty("wepush.http.maxConcurrency." + MESSAGE_TYPE);
        ProviderTrafficController.invalidate(MESSAGE_TYPE, ACCOUNT_ID);
    }

    @Test
    public void shouldShareConcurrencyLimitAcrossSenderInstances() throws Exception {
        System.setProperty("wepush.http.maxConcurrency." + MESSAGE_TYPE, "2");
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(2);
        IMsgSender first = ProviderTrafficController.limit(new BlockingSender(active, peak, started), MESSAGE_TYPE, ACCOUNT_ID);
        // 已经创建的包装器也必须共享新的限制，不能因配置变更拆成两个闸门。
        System.setProperty("wepush.http.maxConcurrency." + MESSAGE_TYPE, "1");
        IMsgSender second = ProviderTrafficController.limit(new BlockingSender(active, peak, started), MESSAGE_TYPE, ACCOUNT_ID);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<SendResult> firstResult = executor.submit(() -> first.send(new String[]{"one"}));
            Future<SendResult> secondResult = executor.submit(() -> second.send(new String[]{"two"}));
            assertTrue(firstResult.get(5, TimeUnit.SECONDS).isSuccess());
            assertTrue(secondResult.get(5, TimeUnit.SECONDS).isSuccess());
            assertEquals(1, peak.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private record BlockingSender(AtomicInteger active, AtomicInteger peak, CountDownLatch started) implements IMsgSender {
        @Override
        public SendResult send(String[] msgData) {
            started.countDown();
            int current = active.incrementAndGet();
            peak.accumulateAndGet(current, Math::max);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                active.decrementAndGet();
            }
            SendResult result = new SendResult();
            result.setSuccess(true);
            return result;
        }

        @Override
        public SendResult asyncSend(String[] msgData) {
            return send(msgData);
        }
    }
}
