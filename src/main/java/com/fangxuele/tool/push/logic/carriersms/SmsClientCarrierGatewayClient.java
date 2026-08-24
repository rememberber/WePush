package com.fangxuele.tool.push.logic.carriersms;

import com.chinamobile.cmos.SmsClient;
import com.chinamobile.cmos.SmsClientBuilder;
import com.fangxuele.tool.push.bean.account.CarrierSmsAccountConfig;
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

/** 基于 sms-client 的四协议长连接实现。 */
public final class SmsClientCarrierGatewayClient implements CarrierSmsGatewayClient {
    private final SmsClient client;
    private final ExecutorService operationExecutor;
    private final AtomicBoolean closed = new AtomicBoolean();

    public SmsClientCarrierGatewayClient(int accountId, CarrierSmsAccountConfig config) {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWaitMillis(config.getRequestTimeoutMillis());
        client = new SmsClientBuilder()
                .entity(CarrierSmsEndpointFactory.create(accountId, config))
                .config(poolConfig)
                .keepAllIdleConnection()
                .build();
        if (client == null) {
            throw new IllegalStateException("创建运营商短信客户端失败");
        }
        operationExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("carrier-sms-" + accountId + "-", 0).factory());
    }

    @Override
    public void connect(int timeoutMillis) throws Exception {
        Boolean connected = executeWithTimeout(client::open, timeoutMillis, "登录网关");
        if (!Boolean.TRUE.equals(connected)) {
            throw new IOException("网关登录失败");
        }
    }

    @Override
    public List<BaseMessage> submit(BaseMessage request, int timeoutMillis) throws Exception {
        return executeWithTimeout(() -> submitAndAwait(request), timeoutMillis, "等待网关提交应答");
    }

    private <T> T executeWithTimeout(Callable<T> operation, int timeoutMillis, String operationName) throws Exception {
        if (closed.get()) {
            throw new IllegalStateException("运营商短信客户端已关闭");
        }
        Future<T> task = operationExecutor.submit(operation);
        try {
            return task.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            task.cancel(true);
            throw new TimeoutException(operationName + "超时（" + timeoutMillis + " ms）");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new IllegalStateException(cause);
        } catch (InterruptedException e) {
            task.cancel(true);
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private List<BaseMessage> submitAndAwait(BaseMessage request) throws Exception {
        List<Promise<BaseMessage>> promises = client.sendAndWaitAllResponse(request);
        if (promises == null || promises.isEmpty()) {
            return List.of();
        }
        List<BaseMessage> responses = new ArrayList<>(promises.size());
        for (Promise<BaseMessage> promise : promises) {
            promise.await();
            if (!promise.isSuccess()) {
                Throwable cause = promise.cause();
                if (cause instanceof Exception exception) {
                    throw exception;
                }
                if (cause == null) {
                    throw new IllegalStateException("网关提交应答失败");
                }
                throw new IllegalStateException(cause);
            }
            responses.add(promise.getNow());
        }
        return responses;
    }

    @Override
    public void close() throws Exception {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            client.close();
        } finally {
            operationExecutor.shutdownNow();
        }
    }
}
