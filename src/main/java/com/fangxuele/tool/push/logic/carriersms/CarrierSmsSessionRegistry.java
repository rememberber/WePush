package com.fangxuele.tool.push.logic.carriersms;

import com.fangxuele.tool.push.bean.account.CarrierSmsAccountConfig;
import com.zx.sms.BaseMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
            return entry.submit(request, config.getRequestTimeoutMillis());
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
            if (current != null) {
                log.info("运营商短信账号配置已变化，重建连接池，accountId={}", id);
            }
            log.info("创建运营商短信连接池，accountId={}，protocol={}，gateway={}:{}，maxChannels={}，window={}",
                    id, config.getProtocol(), config.getHost(), config.getPort(), config.getMaxChannels(),
                    config.getWindowSize());
            Entry replacement = new Entry(fingerprint, clientFactory.create(id, config));
            closeQuietly(current, id);
            return replacement;
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
    }

    private static void closeQuietly(Entry entry, int accountId) {
        if (entry == null) {
            return;
        }
        try {
            entry.close();
        } catch (Exception e) {
            log.warn("关闭运营商短信账号连接失败，accountId={}", accountId, e);
        }
    }

    private static final class Entry {
        private final String fingerprint;
        private final CarrierSmsGatewayClient client;
        private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();

        private Entry(String fingerprint, CarrierSmsGatewayClient client) {
            this.fingerprint = fingerprint;
            this.client = client;
        }

        private String fingerprint() {
            return fingerprint;
        }

        private List<BaseMessage> submit(BaseMessage request, int timeoutMillis) throws Exception {
            lifecycleLock.readLock().lock();
            try {
                return client.submit(request, timeoutMillis);
            } finally {
                lifecycleLock.readLock().unlock();
            }
        }

        private void close() throws Exception {
            lifecycleLock.writeLock().lock();
            try {
                client.close();
            } finally {
                lifecycleLock.writeLock().unlock();
            }
        }
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
