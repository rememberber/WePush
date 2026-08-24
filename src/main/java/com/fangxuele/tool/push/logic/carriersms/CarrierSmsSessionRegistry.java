package com.fangxuele.tool.push.logic.carriersms;

import com.fangxuele.tool.push.bean.account.CarrierSmsAccountConfig;
import com.zx.sms.BaseMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** 每个账号共享一个协议客户端，避免 Classic 工作线程重复建连。 */
@Slf4j
public final class CarrierSmsSessionRegistry {
    private static final ConcurrentHashMap<Integer, Entry> CLIENTS = new ConcurrentHashMap<>();
    private static volatile ClientFactory clientFactory = SmsClientCarrierGatewayClient::new;

    private CarrierSmsSessionRegistry() {
    }

    public static List<BaseMessage> submit(int accountId, CarrierSmsAccountConfig config, BaseMessage request) throws Exception {
        Entry entry = getOrCreate(accountId, config);
        try {
            return entry.client().submit(request, config.getRequestTimeoutMillis());
        } catch (Exception e) {
            // 异常后丢弃当前连接池，下次发送会重建，不在本次自动重发。
            invalidate(accountId, entry);
            throw e;
        }
    }

    private static Entry getOrCreate(int accountId, CarrierSmsAccountConfig config) {
        String fingerprint = config.connectionFingerprint();
        return CLIENTS.compute(accountId, (id, current) -> {
            if (current != null && current.fingerprint().equals(fingerprint)) {
                return current;
            }
            closeQuietly(current, id);
            return new Entry(fingerprint, clientFactory.create(id, config));
        });
    }

    public static void invalidate(int accountId) {
        closeQuietly(CLIENTS.remove(accountId), accountId);
    }

    private static void invalidate(int accountId, Entry expected) {
        if (CLIENTS.remove(accountId, expected)) {
            closeQuietly(expected, accountId);
        }
    }

    public static void shutdown() {
        CLIENTS.forEach((accountId, entry) -> closeQuietly(entry, accountId));
        CLIENTS.clear();
        SmsClientCarrierGatewayClient.shutdownExecutor();
    }

    private static void closeQuietly(Entry entry, int accountId) {
        if (entry == null) {
            return;
        }
        try {
            entry.client().close();
        } catch (Exception e) {
            log.warn("关闭运营商短信账号连接失败，accountId={}", accountId, e);
        }
    }

    private record Entry(String fingerprint, CarrierSmsGatewayClient client) {
    }

    @FunctionalInterface
    interface ClientFactory {
        CarrierSmsGatewayClient create(int accountId, CarrierSmsAccountConfig config);
    }

    static void setClientFactoryForTests(ClientFactory factory) {
        CLIENTS.forEach((accountId, entry) -> closeQuietly(entry, accountId));
        CLIENTS.clear();
        clientFactory = factory;
    }

    static void resetForTests() {
        CLIENTS.forEach((accountId, entry) -> closeQuietly(entry, accountId));
        CLIENTS.clear();
        clientFactory = SmsClientCarrierGatewayClient::new;
    }
}
