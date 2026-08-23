package com.fangxuele.wepush.next.agent.app;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.AgentProtoMapper;
import com.fangxuele.wepush.next.agent.protocol.ProviderCapability;
import com.fangxuele.wepush.next.agent.protocol.v1.AgentControlServiceGrpc;
import com.fangxuele.wepush.next.agent.protocol.v1.AgentToService;
import com.fangxuele.wepush.next.agent.protocol.v1.ServiceToAgent;
import com.fangxuele.wepush.next.agent.runtime.AgentRuntime;
import com.fangxuele.wepush.next.agent.runtime.InboundSequenceResult;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class GrpcAgentClient implements AutoCloseable {
    private static final Metadata.Key<String> TOKEN = Metadata.Key.of(
            "x-wepush-agent-token", Metadata.ASCII_STRING_MARSHALLER);

    private final ManagedChannel channel;
    private final AgentControlServiceGrpc.AgentControlServiceStub stub;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("wepush-agent-heartbeat").factory());

    GrpcAgentClient(String host, int port, boolean plaintext, String token, int maximumMessageBytes) {
        if (host == null || host.isBlank() || port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Service host and Agent gRPC port are invalid");
        }
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(host, port)
                .maxInboundMessageSize(maximumMessageBytes)
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS);
        if (plaintext) builder.usePlaintext();
        else builder.useTransportSecurity();
        channel = builder.build();
        AgentControlServiceGrpc.AgentControlServiceStub configured =
                AgentControlServiceGrpc.newStub(channel);
        if (token != null && !token.isBlank()) {
            Metadata metadata = new Metadata();
            metadata.put(TOKEN, token);
            configured = configured.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
        }
        stub = configured;
    }

    void runSession(AgentRuntime runtime, RemoteAgentRunExecutor remoteRuns,
                    List<ProviderCapability> capabilities)
            throws InterruptedException {
        CountDownLatch ended = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<StreamObserver<AgentToService>> requests = new AtomicReference<>();
        AtomicReference<ScheduledFuture<?>> heartbeat = new AtomicReference<>();
        AtomicBoolean welcomed = new AtomicBoolean();
        Object sendLock = new Object();

        StreamObserver<ServiceToAgent> responses = new StreamObserver<>() {
            @Override
            public void onNext(ServiceToAgent value) {
                try {
                    AgentFrames.ServiceToAgent frame = AgentProtoMapper.fromProto(value);
                    InboundSequenceResult result = runtime.accept(frame);
                    if (result == InboundSequenceResult.GAP) {
                        throw new IllegalStateException("Service sequence gap at " + frame.sequence());
                    }
                    if (result == InboundSequenceResult.DUPLICATE) return;
                    RemoteAgentRunExecutor.AgentFrameSender sender = outbound -> send(requests.get(),
                            AgentProtoMapper.toProto(outbound), sendLock);
                    if (frame.payload() instanceof AgentFrames.Welcome welcome
                            && welcomed.compareAndSet(false, true)) {
                        heartbeat.set(heartbeatExecutor.scheduleWithFixedDelay(() -> {
                            try {
                                send(requests.get(), AgentProtoMapper.toProto(runtime.heartbeat()), sendLock);
                            } catch (RuntimeException problem) {
                                StreamObserver<AgentToService> current = requests.get();
                                if (current != null) current.onError(problem);
                            }
                        }, welcome.heartbeatSeconds(), welcome.heartbeatSeconds(), TimeUnit.SECONDS));
                    } else if (frame.payload() instanceof AgentFrames.LeaseOffer offer) {
                        remoteRuns.offer(offer, sender);
                    } else if (frame.payload() instanceof AgentFrames.RunCommand command) {
                        remoteRuns.command(command, sender);
                    }
                } catch (RuntimeException problem) {
                    StreamObserver<AgentToService> current = requests.get();
                    if (current != null) current.onError(
                            Status.FAILED_PRECONDITION.withDescription(problem.getMessage())
                                    .withCause(problem).asRuntimeException());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                failure.set(throwable);
                cancel(heartbeat.get());
                ended.countDown();
            }

            @Override
            public void onCompleted() {
                cancel(heartbeat.get());
                ended.countDown();
            }
        };

        StreamObserver<AgentToService> request = stub.connect(responses);
        requests.set(request);
        send(request, AgentProtoMapper.toProto(
                runtime.hello(capabilities, remoteRuns.secretEncryptionPublicKey())), sendLock);
        try {
            ended.await();
        } catch (InterruptedException interrupted) {
            request.onError(Status.CANCELLED.withDescription("Agent is shutting down")
                    .withCause(interrupted).asRuntimeException());
            throw interrupted;
        } finally {
            cancel(heartbeat.get());
        }
        Throwable problem = failure.get();
        if (problem != null) throw new AgentConnectionException(problem);
    }

    private static void send(StreamObserver<AgentToService> requests, AgentToService frame,
                             Object sendLock) {
        if (requests == null) return;
        synchronized (sendLock) {
            requests.onNext(frame);
        }
    }

    private static void cancel(ScheduledFuture<?> heartbeat) {
        if (heartbeat != null) heartbeat.cancel(false);
    }

    @Override
    public void close() {
        heartbeatExecutor.shutdownNow();
        channel.shutdown();
        try {
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) channel.shutdownNow();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            channel.shutdownNow();
        }
    }

    static final class AgentConnectionException extends RuntimeException {
        AgentConnectionException(Throwable cause) {
            super(cause);
        }
    }
}
