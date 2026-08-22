package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.AgentProtoMapper;
import com.fangxuele.wepush.next.agent.protocol.v1.AgentControlServiceGrpc;
import com.fangxuele.wepush.next.agent.protocol.v1.AgentToService;
import com.fangxuele.wepush.next.agent.protocol.v1.ServiceToAgent;
import com.fangxuele.wepush.next.service.application.AgentApplicationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

final class AgentControlGrpcService extends AgentControlServiceGrpc.AgentControlServiceImplBase {
    private final AgentApplicationService agents;
    private final ConcurrentHashMap<String, AgentStream> streams = new ConcurrentHashMap<>();

    AgentControlGrpcService(AgentApplicationService agents) {
        this.agents = agents;
    }

    @Override
    public StreamObserver<AgentToService> connect(StreamObserver<ServiceToAgent> responses) {
        return new AgentStream(responses);
    }

    private final class AgentStream implements StreamObserver<AgentToService> {
        private final StreamObserver<ServiceToAgent> responses;
        private final AtomicBoolean terminated = new AtomicBoolean();
        private AgentApplicationService.Connection connection;

        private AgentStream(StreamObserver<ServiceToAgent> responses) {
            this.responses = responses;
        }

        @Override
        public synchronized void onNext(AgentToService value) {
            if (terminated.get()) return;
            try {
                AgentFrames.AgentToService frame = AgentProtoMapper.fromProto(value);
                if (connection == null) {
                    connection = agents.connect(frame);
                    AgentStream previous = streams.put(frame.agentId().value(), this);
                    if (previous != null && previous != this) previous.replaced();
                    responses.onNext(AgentProtoMapper.toProto(connection.welcome()));
                } else {
                    agents.accept(connection, frame);
                }
            } catch (AgentApplicationService.AgentProtocolProblem problem) {
                fail(Status.FAILED_PRECONDITION.withDescription(
                        problem.code() + ": " + problem.getMessage()));
            } catch (IllegalArgumentException problem) {
                fail(Status.INVALID_ARGUMENT.withDescription(problem.getMessage()));
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

        private void terminate(boolean complete, Status failure) {
            if (!terminated.compareAndSet(false, true)) return;
            if (connection != null) {
                streams.remove(connection.registration().id(), this);
                agents.disconnect(connection);
            }
            if (complete) responses.onCompleted();
            else if (failure != null) responses.onError(failure.asRuntimeException());
        }
    }
}
