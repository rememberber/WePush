package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.AgentIdentityService;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

final class AgentGrpcServer implements SmartLifecycle {
    private final String address;
    private final int port;
    private final String token;
    private final int maximumMessageBytes;
    private final AgentControlGrpcService service;
    private final AgentIdentityService identities;
    private final TlsConfiguration tls;
    private volatile Server server;
    private volatile boolean running;

    AgentGrpcServer(String address, int port, String token, long maximumMessageBytes,
                    AgentControlGrpcService service, AgentIdentityService identities,
                    TlsConfiguration tls) {
        if (port < 0 || port > 65_535) throw new IllegalArgumentException("Agent gRPC port is invalid");
        if (maximumMessageBytes < 1024 || maximumMessageBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Agent gRPC maximum message size is invalid");
        }
        if (!isLoopback(address) && (tls == null || !tls.enabled())) {
            throw new IllegalStateException(
                    "Agent gRPC TLS is required when binding outside loopback");
        }
        this.address = address;
        this.port = port;
        this.token = token;
        this.maximumMessageBytes = Math.toIntExact(maximumMessageBytes);
        this.service = service;
        this.identities = identities;
        this.tls = tls == null ? TlsConfiguration.disabled() : tls;
    }

    @Override
    public synchronized void start() {
        if (running) return;
        try {
            NettyServerBuilder builder = NettyServerBuilder.forAddress(new InetSocketAddress(address, port))
                    .maxInboundMessageSize(maximumMessageBytes)
                    .keepAliveTime(Duration.ofSeconds(30).toNanos(), TimeUnit.NANOSECONDS)
                    .keepAliveTimeout(Duration.ofSeconds(10).toNanos(), TimeUnit.NANOSECONDS)
                    .permitKeepAliveTime(Duration.ofSeconds(10).toNanos(), TimeUnit.NANOSECONDS)
                    .addService(ServerInterceptors.intercept(
                            service, new AgentTokenServerInterceptor(
                                    token, identities, isLoopback(address))));
            if (tls.enabled()) builder.sslContext(sslContext());
            server = builder.build().start();
            running = true;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot start Agent gRPC server on " + address + ":" + port,
                    exception);
        }
    }

    @Override
    public synchronized void stop() {
        if (server == null) return;
        server.shutdown();
        try {
            if (!server.awaitTermination(5, TimeUnit.SECONDS)) server.shutdownNow();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            server.shutdownNow();
        } finally {
            running = false;
            server = null;
        }
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    int localPort() {
        Server current = server;
        return current == null ? -1 : current.getPort();
    }

    private io.grpc.netty.shaded.io.netty.handler.ssl.SslContext sslContext() {
        try {
            requireFile(tls.certificateChain(), "Agent gRPC TLS certificate chain");
            requireFile(tls.privateKey(), "Agent gRPC TLS private key");
            SslContextBuilder builder = GrpcSslContexts.forServer(
                    tls.certificateChain().toFile(), tls.privateKey().toFile());
            if (tls.trustCertificates() != null) {
                requireFile(tls.trustCertificates(), "Agent gRPC TLS trust certificates");
                builder.trustManager(tls.trustCertificates().toFile());
            }
            if (tls.requireClientCertificate()) {
                if (tls.trustCertificates() == null) {
                    throw new IllegalStateException(
                            "Agent gRPC mTLS requires a trust certificate collection");
                }
                builder.clientAuth(ClientAuth.REQUIRE);
            }
            return builder.build();
        } catch (IOException problem) {
            throw new IllegalStateException("Agent gRPC TLS configuration is invalid", problem);
        }
    }

    private static void requireFile(Path path, String label) {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalStateException(label + " is missing: " + path);
        }
    }

    static boolean isLoopback(String address) {
        try {
            return InetAddress.getByName(address).isLoopbackAddress();
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Agent gRPC address cannot be resolved: " + address, exception);
        }
    }

    record TlsConfiguration(boolean enabled, Path certificateChain, Path privateKey,
                            Path trustCertificates, boolean requireClientCertificate) {
        static TlsConfiguration disabled() {
            return new TlsConfiguration(false, null, null, null, false);
        }
    }
}
