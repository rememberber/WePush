package com.fangxuele.tool.push.logic.carriersms;

import com.chinamobile.cmos.SmsClient;
import com.chinamobile.cmos.SmsClientBuilder;
import com.fangxuele.tool.push.bean.account.CarrierSmsAccountConfig;
import com.zx.sms.BaseMessage;
import io.netty.util.concurrent.Promise;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** 基于 sms-client 的四协议长连接实现。 */
public final class SmsClientCarrierGatewayClient implements CarrierSmsGatewayClient {
    private static final ExecutorService SUBMIT_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final SmsClient client;

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
    }

    @Override
    public List<BaseMessage> submit(BaseMessage request, int timeoutMillis) throws Exception {
        Future<List<BaseMessage>> task = SUBMIT_EXECUTOR.submit(() -> submitAndAwait(request));
        try {
            return task.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            task.cancel(true);
            throw new TimeoutException("等待网关提交应答超时（" + timeoutMillis + " ms）");
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
        client.close();
    }

    static void shutdownExecutor() {
        SUBMIT_EXECUTOR.shutdownNow();
    }
}
