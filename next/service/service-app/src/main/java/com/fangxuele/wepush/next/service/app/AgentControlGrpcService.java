package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.AgentProtoMapper;
import com.fangxuele.wepush.next.agent.protocol.v1.AgentControlServiceGrpc;
import com.fangxuele.wepush.next.agent.protocol.v1.AgentToService;
import com.fangxuele.wepush.next.agent.protocol.v1.ServiceToAgent;
import com.fangxuele.wepush.next.service.application.AgentApplicationService;
import com.fangxuele.wepush.next.service.application.RemoteRunCoordinator;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;

final class AgentControlGrpcService extends AgentControlServiceGrpc.AgentControlServiceImplBase {
    private final AgentApplicationService agents;
    private final AgentStreamGateway streams;
    private final RemoteRunCoordinator remoteRuns;
    private final boolean remoteExecution;

    AgentControlGrpcService(AgentApplicationService agents, AgentStreamGateway streams,
                            RemoteRunCoordinator remoteRuns, boolean remoteExecution) {
        this.agents = agents;
        this.streams = streams;
        this.remoteRuns = remoteRuns;
        this.remoteExecution = remoteExecution;
    }

    @Override
    public StreamObserver<AgentToService> connect(StreamObserver<ServiceToAgent> responses) {
        return new AgentStream(responses, AgentTokenServerInterceptor.AUTHENTICATED_AGENT_ID.get());
    }

    private final class AgentStream implements StreamObserver<AgentToService> {
        private final StreamObserver<ServiceToAgent> responses;
        private final AtomicBoolean terminated = new AtomicBoolean();
        private final String authenticatedAgentId;
        private AgentApplicationService.Connection connection;

        private AgentStream(StreamObserver<ServiceToAgent> responses, String authenticatedAgentId) {
            this.responses = responses;
            this.authenticatedAgentId = authenticatedAgentId == null ? "" : authenticatedAgentId;
        }

        @Override
        public synchronized void onNext(AgentToService value) {
            if (terminated.get()) return;
            try {
                AgentFrames.AgentToService frame = AgentProtoMapper.fromProto(value);
                if (connection == null) {
                    if (!authenticatedAgentId.isEmpty()
                            && !authenticatedAgentId.equals(frame.agentId().value())) {
                        throw new AgentApplicationService.AgentProtocolProblem(
                                "AGENT_IDENTITY_MISMATCH",
                                "Authenticated Agent identity differs from Hello");
                    }
                    connection = agents.connect(frame);
                    streams.register(connection, this::send, this::replaced);
                    List<com.fangxuele.wepush.next.agent.protocol.LeaseFence> resumable =
                            remoteExecution ? remoteRuns.reconnected(connection.registration(),
                                    connection.recoveredLeases()) : List.of();
                    responses.onNext(AgentProtoMapper.toProto(connection.welcome(resumable)));
                    if (remoteExecution) {
                        remoteRuns.deliverPendingForAgent(connection.registration().id());
                        remoteRuns.recoverPending();
                    }
                } else {
                    AtomicReference<AgentFrames.ServicePayload> response = new AtomicReference<>();
                    agents.accept(connection, frame, payload -> {
                        if (remoteExecution) {
                            remoteRuns.accept(connection.registration(), payload).ifPresent(response::set);
                        }
                    });
                    if (response.get() != null
                            && !streams.send(connection.registration().id(), response.get())) {
                        throw new IllegalStateException("Agent response stream is unavailable");
                    }
                }
            } catch (AgentApplicationService.AgentProtocolProblem problem) {
                fail(Status.FAILED_PRECONDITION.withDescription(
                        problem.code() + ": " + problem.getMessage()));
            } catch (IllegalArgumentException problem) {
                fail(Status.INVALID_ARGUMENT.withDescription(problem.getMessage()));
            } catch (RemoteRunCoordinator.RemoteProtocolProblem problem) {
                fail(Status.FAILED_PRECONDITION.withDescription(
                        problem.code() + ": " + problem.getMessage()));
            } catch (IllegalStateException problem) {
                fail(Status.ABORTED.withDescription(problem.getMessage()));
            } catch (RuntimeException problem) {
                fail(Status.INTERNAL.withDescription("Agent control processing failed")
                        .withCause(problem));
            }
        }

        @Override
        public void onError(Throwable throwable) {
            terminate(false, null);
        }

        @Override
        public void onCompleted() {
            terminate(true, null);
        }

        private void replaced() {
            fail(Status.ABORTED.withDescription("Agent connected with a newer session"));
        }

        private void fail(Status status) {
            terminate(false, status);
        }

        private synchronized void send(AgentFrames.ServiceToAgent frame) {
            if (terminated.get()) throw new IllegalStateException("Agent stream is terminated");
            responses.onNext(AgentProtoMapper.toProto(frame));
        }

        private void terminate(boolean complete, Status failure) {
            if (!terminated.compareAndSet(false, true)) return;
            if (connection != null) {
                streams.unregister(connection);
                agents.disconnect(connection);
                if (remoteExecution) remoteRuns.disconnected(connection.registration());
            }
            if (complete) responses.onCompleted();
            else if (failure != null) responses.onError(failure.asRuntimeException());
        }
    }
}
