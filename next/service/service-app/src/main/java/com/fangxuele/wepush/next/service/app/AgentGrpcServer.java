package com.fangxuele.wepush.next.service.app;

import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

final class AgentGrpcServer implements SmartLifecycle {
    private final String address;
    private final int port;
    private final String token;
    private final int maximumMessageBytes;
    private final AgentControlGrpcService service;
    private volatile Server server;
    private volatile boolean running;

    AgentGrpcServer(String address, int port, String token, long maximumMessageBytes,
                    AgentControlGrpcService service) {
        if (port < 0 || port > 65_535) throw new IllegalArgumentException("Agent gRPC port is invalid");
        if (maximumMessageBytes < 1024 || maximumMessageBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Agent gRPC maximum message size is invalid");
        }
        if ((token == null || token.isBlank()) && !isLoopback(address)) {
            throw new IllegalStateException(
                    "WEPUSH_AGENT_GRPC_TOKEN is required when Agent gRPC is not bound to loopback");
        }
        this.address = address;
        this.port = port;
        this.token = token;
        this.maximumMessageBytes = Math.toIntExact(maximumMessageBytes);
        this.service = service;
    }

    @Override
    public synchronized void start() {
        if (running) return;
        try {
            server = NettyServerBuilder.forAddress(new InetSocketAddress(address, port))
                    .maxInboundMessageSize(maximumMessageBytes)
                    .keepAliveTime(Duration.ofSeconds(30).toNanos(), TimeUnit.NANOSECONDS)
                    .keepAliveTimeout(Duration.ofSeconds(10).toNanos(), TimeUnit.NANOSECONDS)
                    .permitKeepAliveTime(Duration.ofSeconds(10).toNanos(), TimeUnit.NANOSECONDS)
                    .addService(ServerInterceptors.intercept(
                            service, new AgentTokenServerInterceptor(token)))
                    .build()
                    .start();
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

    private static boolean isLoopback(String address) {
        try {
            return InetAddress.getByName(address).isLoopbackAddress();
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Agent gRPC address cannot be resolved: " + address, exception);
        }
    }
}
