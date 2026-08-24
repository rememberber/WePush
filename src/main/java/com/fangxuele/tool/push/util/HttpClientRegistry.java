package com.fangxuele.tool.push.util;

import com.fangxuele.tool.push.App;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * 全局 HTTP 客户端注册表。客户端按消息类型、账号和网络配置隔离，并在同一账号内共享连接池。
 */
@Slf4j
public final class HttpClientRegistry {

    public static final String PROTOCOL_PROPERTY = "wepush.http.protocol";

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_WRITE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(60);
    private static final long[] LATENCY_BUCKETS_MILLIS = {
            5, 10, 20, 50, 100, 200, 500, 1_000, 2_000, 5_000, 10_000, 30_000, 60_000, Long.MAX_VALUE
    };

    private static final Map<ClientKey, OkHttpClient> CLIENTS = new ConcurrentHashMap<>();
    private static final Map<AccountKey, MetricsBucket> METRICS = new ConcurrentHashMap<>();

    private HttpClientRegistry() {
    }

    public static OkHttpClient get(int messageType, int accountId) {
        return get(messageType, accountId, ClientOptions.defaults());
    }

    public static OkHttpClient getInsecure(int messageType, int accountId) {
        return get(messageType, accountId, ClientOptions.defaults().withTrustAllCertificates(true));
    }

    public static OkHttpClient get(int messageType, int accountId, ClientOptions options) {
        Objects.requireNonNull(options, "options");
        ProtocolMode protocolMode = ProtocolMode.fromProperty(System.getProperty(PROTOCOL_PROPERTY));
        ClientKey key = ClientKey.from(messageType, accountId, options, protocolMode);
        return CLIENTS.computeIfAbsent(key, ignored -> buildClient(key.accountKey(), options, protocolMode));
    }

    public static void invalidate(int messageType, int accountId) {
        CLIENTS.entrySet().removeIf(entry -> {
            ClientKey key = entry.getKey();
            if (key.messageType() == messageType && key.accountId() == accountId) {
                close(entry.getValue());
                return true;
            }
            return false;
        });
        METRICS.remove(new AccountKey(messageType, accountId));
    }

    public static void invalidateAccount(int accountId) {
        CLIENTS.entrySet().removeIf(entry -> {
            if (entry.getKey().accountId() == accountId) {
                close(entry.getValue());
                return true;
            }
            return false;
        });
        METRICS.keySet().removeIf(key -> key.accountId() == accountId);
    }

    public static MetricsSnapshot snapshot(int messageType, int accountId) {
        MetricsBucket bucket = METRICS.get(new AccountKey(messageType, accountId));
        return bucket == null ? MetricsSnapshot.empty() : bucket.snapshot();
    }

    /** 汇总一个账号的所有通道指标，用于 WxJava 多种消息共享同一服务实例的场景。 */
    public static MetricsSnapshot snapshotAccount(int accountId) {
        MetricsSnapshot total = MetricsSnapshot.empty();
        for (Map.Entry<AccountKey, MetricsBucket> entry : METRICS.entrySet()) {
            if (entry.getKey().accountId() == accountId) {
                total = total.plus(entry.getValue().snapshot());
            }
        }
        return total;
    }

    public static String formatDelta(int messageType, int accountId, MetricsSnapshot before) {
        return format(snapshot(messageType, accountId).minus(before));
    }

    public static String formatAccountDelta(int accountId, MetricsSnapshot before) {
        return format(snapshotAccount(accountId).minus(before));
    }

    private static String format(MetricsSnapshot delta) {
        if (delta.calls() == 0) {
            return "";
        }
        return String.format(Locale.ROOT,
                "HTTP指标：请求=%d，失败=%d，HTTP/2=%d，HTTP/1.1=%d，新建连接=%d，复用连接=%d，TLS握手=%d，429=%d，p50<=%dms，p95<=%dms，p99<=%dms",
                delta.calls(), delta.failures(), delta.http2Calls(), delta.http1Calls(), delta.connections(),
                delta.reusedConnections(), delta.tlsHandshakes(), delta.throttledResponses(),
                delta.percentileMillis(0.50), delta.percentileMillis(0.95), delta.percentileMillis(0.99));
    }

    public static void shutdown() {
        CLIENTS.values().forEach(HttpClientRegistry::close);
        CLIENTS.clear();
        METRICS.clear();
    }

    static OkHttpClient buildClient(AccountKey accountKey, ClientOptions options, ProtocolMode protocolMode) {
        int maxRequests = configuredMaxThreads();
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(maxRequests);
        dispatcher.setMaxRequestsPerHost(maxRequests);

        // maxIdleConnections 只控制空闲连接，不是并发上限；并发由 ProviderTrafficController 管理。
        int maxIdleConnections = Math.max(5, Math.min(64, maxRequests));
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(maxIdleConnections, 5, TimeUnit.MINUTES))
                .connectTimeout(options.connectTimeout())
                .readTimeout(options.readTimeout())
                .writeTimeout(options.writeTimeout())
                .callTimeout(options.callTimeout())
                .retryOnConnectionFailure(true)
                .eventListenerFactory(call -> new MetricsEventListener(accountKey));

        if (protocolMode == ProtocolMode.HTTP_1_ONLY) {
            builder.protocols(List.of(Protocol.HTTP_1_1));
        }
        if (options.proxy() != null) {
            builder.proxy(options.proxy());
            if (options.proxyUsername() != null && !options.proxyUsername().isBlank()) {
                builder.proxyAuthenticator((route, response) -> {
                    if (response.request().header("Proxy-Authorization") != null) {
                        return null;
                    }
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", okhttp3.Credentials.basic(
                                    options.proxyUsername(), Objects.toString(options.proxyPassword(), "")))
                            .build();
                });
            }
        }
        if (options.trustAllCertificates()) {
            configureTrustAll(builder);
            log.warn("账号 {}-{} 使用了兼容模式 TLS（忽略证书和主机名校验）", accountKey.messageType(), accountKey.accountId());
        }
        log.info("初始化共享 HTTP 客户端：消息类型={}，账号={}，协议模式={}",
                accountKey.messageType(), accountKey.accountId(), protocolMode);
        return builder.build();
    }

    private static int configuredMaxThreads() {
        try {
            return Math.max(1, App.config.getMaxThreads());
        } catch (Exception ignored) {
            return 100;
        }
    }

    private static void configureTrustAll(OkHttpClient.Builder builder) {
        try {
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .hostnameVerifier((hostname, session) -> true);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("无法初始化兼容模式 TLS", e);
        }
    }

    private static void close(OkHttpClient client) {
        try {
            client.dispatcher().cancelAll();
            client.connectionPool().evictAll();
            client.dispatcher().executorService().shutdown();
        } catch (Exception e) {
            log.warn("关闭 HTTP 客户端失败", e);
        }
    }

    public enum ProtocolMode {
        AUTO,
        HTTP_1_ONLY;

        static ProtocolMode fromProperty(String value) {
            if (value == null) {
                return AUTO;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            return normalized.equals("http1") || normalized.equals("http/1.1") || normalized.equals("h1")
                    ? HTTP_1_ONLY : AUTO;
        }
    }

    public record ClientOptions(Proxy proxy,
                                String proxyUsername,
                                String proxyPassword,
                                Duration connectTimeout,
                                Duration readTimeout,
                                Duration writeTimeout,
                                Duration callTimeout,
                                boolean trustAllCertificates) {

        public ClientOptions {
            connectTimeout = nonNullDuration(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
            readTimeout = nonNullDuration(readTimeout, DEFAULT_READ_TIMEOUT);
            writeTimeout = nonNullDuration(writeTimeout, DEFAULT_WRITE_TIMEOUT);
            callTimeout = nonNullDuration(callTimeout, DEFAULT_CALL_TIMEOUT);
        }

        public static ClientOptions defaults() {
            return new ClientOptions(null, null, null, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT,
                    DEFAULT_WRITE_TIMEOUT, DEFAULT_CALL_TIMEOUT, false);
        }

        public ClientOptions withProxy(Proxy proxy, String username, String password) {
            return new ClientOptions(proxy, username, password, connectTimeout, readTimeout, writeTimeout,
                    callTimeout, trustAllCertificates);
        }

        public ClientOptions withTimeouts(Duration connect, Duration read, Duration write, Duration call) {
            return new ClientOptions(proxy, proxyUsername, proxyPassword, connect, read, write, call,
                    trustAllCertificates);
        }

        public ClientOptions withTrustAllCertificates(boolean trustAll) {
            return new ClientOptions(proxy, proxyUsername, proxyPassword, connectTimeout, readTimeout,
                    writeTimeout, callTimeout, trustAll);
        }

        private static Duration nonNullDuration(Duration value, Duration fallback) {
            return value == null ? fallback : value;
        }
    }

    public record MetricsSnapshot(long calls,
                                  long failures,
                                  long http2Calls,
                                  long http1Calls,
                                  long connections,
                                  long reusedConnections,
                                  long tlsHandshakes,
                                  long throttledResponses,
                                  long[] latencyBuckets) {

        public MetricsSnapshot {
            latencyBuckets = latencyBuckets.clone();
        }

        static MetricsSnapshot empty() {
            return new MetricsSnapshot(0, 0, 0, 0, 0, 0, 0, 0,
                    new long[LATENCY_BUCKETS_MILLIS.length]);
        }

        MetricsSnapshot minus(MetricsSnapshot before) {
            long[] buckets = new long[latencyBuckets.length];
            for (int i = 0; i < buckets.length; i++) {
                buckets[i] = Math.max(0, latencyBuckets[i] - before.latencyBuckets[i]);
            }
            return new MetricsSnapshot(
                    Math.max(0, calls - before.calls),
                    Math.max(0, failures - before.failures),
                    Math.max(0, http2Calls - before.http2Calls),
                    Math.max(0, http1Calls - before.http1Calls),
                    Math.max(0, connections - before.connections),
                    Math.max(0, reusedConnections - before.reusedConnections),
                    Math.max(0, tlsHandshakes - before.tlsHandshakes),
                    Math.max(0, throttledResponses - before.throttledResponses),
                    buckets);
        }

        MetricsSnapshot plus(MetricsSnapshot other) {
            long[] buckets = new long[latencyBuckets.length];
            for (int i = 0; i < buckets.length; i++) {
                buckets[i] = latencyBuckets[i] + other.latencyBuckets[i];
            }
            return new MetricsSnapshot(calls + other.calls, failures + other.failures,
                    http2Calls + other.http2Calls, http1Calls + other.http1Calls,
                    connections + other.connections, reusedConnections + other.reusedConnections,
                    tlsHandshakes + other.tlsHandshakes, throttledResponses + other.throttledResponses,
                    buckets);
        }

        long percentileMillis(double percentile) {
            if (calls == 0) {
                return 0;
            }
            long target = Math.max(1, (long) Math.ceil(calls * percentile));
            long cumulative = 0;
            for (int i = 0; i < latencyBuckets.length; i++) {
                cumulative += latencyBuckets[i];
                if (cumulative >= target) {
                    return LATENCY_BUCKETS_MILLIS[i] == Long.MAX_VALUE
                            ? LATENCY_BUCKETS_MILLIS[LATENCY_BUCKETS_MILLIS.length - 2]
                            : LATENCY_BUCKETS_MILLIS[i];
                }
            }
            return LATENCY_BUCKETS_MILLIS[LATENCY_BUCKETS_MILLIS.length - 2];
        }

        @Override
        public long[] latencyBuckets() {
            return latencyBuckets.clone();
        }
    }

    record AccountKey(int messageType, int accountId) {
    }

    private record ClientKey(int messageType,
                             int accountId,
                             String proxyIdentity,
                             int credentialFingerprint,
                             long connectTimeoutMillis,
                             long readTimeoutMillis,
                             long writeTimeoutMillis,
                             long callTimeoutMillis,
                             boolean trustAllCertificates,
                             ProtocolMode protocolMode) {

        static ClientKey from(int messageType, int accountId, ClientOptions options, ProtocolMode protocolMode) {
            String proxyIdentity = "DIRECT";
            if (options.proxy() != null) {
                proxyIdentity = options.proxy().type() + ":" + options.proxy().address();
            }
            return new ClientKey(messageType, accountId, proxyIdentity,
                    Objects.hash(options.proxyUsername(), options.proxyPassword()),
                    options.connectTimeout().toMillis(), options.readTimeout().toMillis(),
                    options.writeTimeout().toMillis(), options.callTimeout().toMillis(),
                    options.trustAllCertificates(), protocolMode);
        }

        AccountKey accountKey() {
            return new AccountKey(messageType, accountId);
        }
    }

    private static final class MetricsBucket {
        private final LongAdder calls = new LongAdder();
        private final LongAdder failures = new LongAdder();
        private final LongAdder http2Calls = new LongAdder();
        private final LongAdder http1Calls = new LongAdder();
        private final LongAdder connections = new LongAdder();
        private final LongAdder reusedConnections = new LongAdder();
        private final LongAdder tlsHandshakes = new LongAdder();
        private final LongAdder throttledResponses = new LongAdder();
        private final LongAdder[] latencyBuckets = Arrays.stream(LATENCY_BUCKETS_MILLIS)
                .mapToObj(ignored -> new LongAdder()).toArray(LongAdder[]::new);

        MetricsSnapshot snapshot() {
            long[] buckets = new long[latencyBuckets.length];
            for (int i = 0; i < buckets.length; i++) {
                buckets[i] = latencyBuckets[i].sum();
            }
            return new MetricsSnapshot(calls.sum(), failures.sum(), http2Calls.sum(), http1Calls.sum(),
                    connections.sum(), reusedConnections.sum(), tlsHandshakes.sum(), throttledResponses.sum(), buckets);
        }

        void recordLatency(long elapsedMillis) {
            for (int i = 0; i < LATENCY_BUCKETS_MILLIS.length; i++) {
                if (elapsedMillis <= LATENCY_BUCKETS_MILLIS[i]) {
                    latencyBuckets[i].increment();
                    return;
                }
            }
        }
    }

    private static final class MetricsEventListener extends EventListener {
        private final MetricsBucket metrics;
        private long startNanos;
        private boolean connectedDuringCall;
        private boolean completed;

        private MetricsEventListener(AccountKey accountKey) {
            this.metrics = METRICS.computeIfAbsent(accountKey, ignored -> new MetricsBucket());
        }

        @Override
        public void callStart(Call call) {
            startNanos = System.nanoTime();
        }

        @Override
        public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
            connectedDuringCall = true;
            metrics.connections.increment();
        }

        @Override
        public void secureConnectEnd(Call call, Handshake handshake) {
            metrics.tlsHandshakes.increment();
        }

        @Override
        public void connectionAcquired(Call call, Connection connection) {
            Protocol protocol = connection.protocol();
            if (protocol == Protocol.HTTP_2) {
                metrics.http2Calls.increment();
            } else if (protocol == Protocol.HTTP_1_1 || protocol == Protocol.HTTP_1_0) {
                metrics.http1Calls.increment();
            }
            if (!connectedDuringCall) {
                metrics.reusedConnections.increment();
            }
        }

        @Override
        public void responseHeadersEnd(Call call, Response response) {
            if (response.code() == 429) {
                metrics.throttledResponses.increment();
            }
        }

        @Override
        public void callEnd(Call call) {
            complete(false);
        }

        @Override
        public void callFailed(Call call, IOException ioe) {
            complete(true);
        }

        private void complete(boolean failed) {
            if (completed) {
                return;
            }
            completed = true;
            metrics.calls.increment();
            if (failed) {
                metrics.failures.increment();
            }
            metrics.recordLatency(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        }
    }
}
