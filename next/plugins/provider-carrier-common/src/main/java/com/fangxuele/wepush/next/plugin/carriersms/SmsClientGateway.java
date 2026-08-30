package com.fangxuele.wepush.next.plugin.carriersms;

import com.chinamobile.cmos.SmsClient;
import com.chinamobile.cmos.SmsClientBuilder;
import com.zx.sms.BaseMessage;
import io.netty.util.concurrent.Promise;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

final class SmsClientGateway implements AutoCloseable {
    private final SmsClient client;
    private final ExecutorService operations;
    private final AtomicBoolean closed = new AtomicBoolean();

    SmsClientGateway(String sessionId, CarrierSmsConfig config, String password) {
        GenericObjectPoolConfig<?> pool = new GenericObjectPoolConfig<>();
        pool.setBlockWhenExhausted(true); pool.setMaxWaitMillis(config.requestTimeoutMillis());
        client = new SmsClientBuilder().entity(CarrierSmsEndpointFactory.create(sessionId, config, password))
                .config(pool).keepAllIdleConnection().build();
        if (client == null) throw new IllegalStateException("Carrier SMS client could not be created");
        operations = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("wepush-carrier-sms-" + sessionId + "-", 0).factory());
    }

    void connect(int timeoutMillis) throws Exception {
        Boolean connected = execute(client::open, timeoutMillis);
        if (!Boolean.TRUE.equals(connected)) throw new IOException("Carrier gateway rejected login");
    }

    List<BaseMessage> submit(BaseMessage request, int timeoutMillis) throws Exception {
        return execute(() -> {
            List<Promise<BaseMessage>> promises = client.sendAndWaitAllResponse(request);
            if (promises == null || promises.isEmpty()) return List.of();
            List<BaseMessage> responses = new ArrayList<>(promises.size());
            for (Promise<BaseMessage> promise : promises) {
                promise.await();
                if (!promise.isSuccess()) {
                    Throwable cause = promise.cause();
                    if (cause instanceof Exception exception) throw exception;
                    if (cause == null) throw new IllegalStateException("SUBMIT_RESP failed");
                    throw new IllegalStateException(cause);
                }
                responses.add(promise.getNow());
            }
            return responses;
        }, timeoutMillis);
    }

    private <T> T execute(Callable<T> call, int timeoutMillis) throws Exception {
        if (closed.get()) throw new IllegalStateException("Carrier SMS client is closed");
        Future<T> future = operations.submit(call);
        try { return future.get(timeoutMillis, TimeUnit.MILLISECONDS); }
        catch (TimeoutException problem) { future.cancel(true); throw problem; }
        catch (InterruptedException problem) { future.cancel(true); Thread.currentThread().interrupt(); throw problem; }
        catch (ExecutionException problem) {
            if (problem.getCause() instanceof Exception exception) throw exception;
            throw new IllegalStateException(problem.getCause());
        }
    }

    @Override
    public void close() throws Exception {
        if (!closed.compareAndSet(false, true)) return;
        try { client.close(); } finally { operations.shutdownNow(); }
    }
}
