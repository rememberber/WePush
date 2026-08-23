package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.service.domain.AgentRegistration;
import com.fangxuele.wepush.next.service.domain.AgentRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class AgentApplicationService {
    public static final int PROTOCOL_VERSION = 1;

    private final AgentRepository agents;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final Clock clock;
    private final Duration heartbeatInterval;
    private final long maximumMessageBytes;

    public AgentApplicationService(AgentRepository agents, ResourceIdGenerator ids,
                                   TransactionRunner transactions, Clock clock,
                                   Duration heartbeatInterval, long maximumMessageBytes) {
        this.agents = Objects.requireNonNull(agents, "agents");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.heartbeatInterval = positive(heartbeatInterval, "heartbeatInterval");
        if (maximumMessageBytes < 1024 || maximumMessageBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("maximumMessageBytes is outside the supported range");
        }
        this.maximumMessageBytes = maximumMessageBytes;
    }

    public Connection connect(AgentFrames.AgentToService frame) {
        if (!(frame.payload() instanceof AgentFrames.Hello hello)) {
            throw new AgentProtocolProblem("HELLO_REQUIRED", "The first Agent frame must be Hello");
        }
        if (hello.protocolMinimum() > PROTOCOL_VERSION || hello.protocolMaximum() < PROTOCOL_VERSION) {
            throw new AgentProtocolProblem("PROTOCOL_INCOMPATIBLE", "Agent does not support protocol v1");
        }
        Instant now = clock.instant();
        String sessionId = ids.next("agent-session");
        AgentRegistration existing = agents.findById(frame.agentId().value()).orElse(null);
        long serviceSequence = hello.lastServiceSequence() + 1;
        if (existing != null && hello.lastServiceSequence() > existing.lastServiceSequence()) {
            throw new AgentProtocolProblem("SERVICE_SEQUENCE_AHEAD",
                    "Agent reports a Service sequence that was never issued");
        }
        List<AgentRegistration.Provider> providers = hello.providers().stream()
                .map(value -> new AgentRegistration.Provider(value.providerId(),
                        value.implementationVersion(), value.spiMajorVersion(), value.maximumConcurrency()))
                .toList();
        AgentRegistration registration = new AgentRegistration(frame.agentId().value(),
                AgentRegistration.Status.ONLINE, hello.agentVersion(), PROTOCOL_VERSION,
                hello.operatingSystem(), hello.architecture(), hello.javaVersion(), hello.maximumRuns(),
                0, hello.maximumRuns(), providers, hello.secretEncryptionPublicKey(), sessionId,
                frame.sequence(), serviceSequence,
                now, now, null, existing == null ? 0 : existing.version() + 1);
        transactions.required(() -> agents.connect(registration));
        AgentFrames.Welcome welcome = new AgentFrames.Welcome(PROTOCOL_VERSION, now,
                Math.toIntExact(heartbeatInterval.toSeconds()), maximumMessageBytes, frame.sequence());
        return new Connection(registration,
                new AgentFrames.ServiceToAgent(serviceSequence, welcome), hello.recoveredLeases());
    }

    public void accept(Connection connection, AgentFrames.AgentToService frame) {
        accept(connection, frame, _payload -> { });
    }

    public void accept(Connection connection, AgentFrames.AgentToService frame,
                       Consumer<AgentFrames.AgentPayload> processor) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(processor, "processor");
        if (!connection.registration().id().equals(frame.agentId().value())) {
            throw new AgentProtocolProblem("AGENT_ID_CHANGED", "Agent identity changed inside one stream");
        }
        long expected = connection.lastAgentSequence + 1;
        if (frame.sequence() < expected) return;
        if (frame.sequence() > expected) {
            throw new AgentProtocolProblem("AGENT_SEQUENCE_GAP",
                    "Expected Agent sequence " + expected + " but received " + frame.sequence());
        }
        if (frame.payload() instanceof AgentFrames.Hello) {
            throw new AgentProtocolProblem("HELLO_REPEATED", "Hello is only valid as the first frame");
        }
        processor.accept(frame.payload());
        Instant now = clock.instant();
        if (frame.payload() instanceof AgentFrames.Heartbeat heartbeat) {
            AgentRegistration.Status status = switch (heartbeat.state()) {
                case "READY", "ONLINE" -> AgentRegistration.Status.ONLINE;
                case "DRAINING" -> AgentRegistration.Status.DRAINING;
                case "DEGRADED" -> AgentRegistration.Status.DEGRADED;
                default -> throw new AgentProtocolProblem("AGENT_STATE_INVALID",
                        "Unsupported Agent heartbeat state: " + heartbeat.state());
            };
            if (heartbeat.activeRuns() + heartbeat.availableRuns()
                    > connection.registration().maximumRuns()) {
                throw new AgentProtocolProblem("AGENT_CAPACITY_INVALID",
                        "Heartbeat capacity exceeds the Hello maximum");
            }
            transactions.required(() -> agents.heartbeat(connection.registration().id(),
                    connection.registration().sessionId(), status, heartbeat.activeRuns(),
                    heartbeat.availableRuns(), frame.sequence(), now));
        } else {
            transactions.required(() -> agents.advanceSequence(connection.registration().id(),
                    connection.registration().sessionId(), frame.sequence(), now));
        }
        connection.lastAgentSequence = frame.sequence();
    }

    public synchronized AgentFrames.ServiceToAgent next(Connection connection,
                                                        AgentFrames.ServicePayload payload) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(payload, "payload");
        long sequence = connection.lastServiceSequence + 1;
        transactions.required(() -> agents.advanceServiceSequence(connection.registration().id(),
                connection.registration().sessionId(), sequence));
        connection.lastServiceSequence = sequence;
        return new AgentFrames.ServiceToAgent(sequence, payload);
    }

    public void disconnect(Connection connection) {
        if (connection == null) return;
        transactions.required(() -> agents.disconnect(connection.registration().id(),
                connection.registration().sessionId(), clock.instant()));
    }

    public int expireSilent() {
        Instant now = clock.instant();
        Instant cutoff = now.minus(heartbeatInterval.multipliedBy(3));
        return transactions.required(() -> agents.expireSilent(cutoff, now));
    }

    public List<AgentRegistration> list() {
        return agents.list();
    }

    public AgentRegistration get(String agentId) {
        return agents.findById(agentId).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "AGENT_NOT_FOUND",
                        "Agent was not found: " + agentId));
    }

    private static Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative() || value.toSeconds() < 1) {
            throw new IllegalArgumentException(name + " must be at least one second");
        }
        return value;
    }

    public static final class Connection {
        private final AgentRegistration registration;
        private final AgentFrames.ServiceToAgent welcome;
        private final List<com.fangxuele.wepush.next.agent.protocol.LeaseFence> recoveredLeases;
        private long lastAgentSequence;
        private long lastServiceSequence;

        private Connection(AgentRegistration registration, AgentFrames.ServiceToAgent welcome,
                           List<com.fangxuele.wepush.next.agent.protocol.LeaseFence> recoveredLeases) {
            this.registration = registration;
            this.welcome = welcome;
            this.recoveredLeases = List.copyOf(recoveredLeases);
            this.lastAgentSequence = registration.lastAgentSequence();
            this.lastServiceSequence = welcome.sequence();
        }

        public AgentRegistration registration() { return registration; }

        public AgentFrames.ServiceToAgent welcome() { return welcome; }

        public AgentFrames.ServiceToAgent welcome(
                List<com.fangxuele.wepush.next.agent.protocol.LeaseFence> resumableLeases) {
            AgentFrames.Welcome value = (AgentFrames.Welcome) welcome.payload();
            return new AgentFrames.ServiceToAgent(welcome.sequence(), new AgentFrames.Welcome(
                    value.protocolVersion(), value.serverTime(), value.heartbeatSeconds(),
                    value.maximumMessageBytes(), value.lastAgentSequence(), resumableLeases));
        }

        public List<com.fangxuele.wepush.next.agent.protocol.LeaseFence> recoveredLeases() {
            return recoveredLeases;
        }
    }

    public static final class AgentProtocolProblem extends RuntimeException {
        private final String code;

        public AgentProtocolProblem(String code, String message) {
            super(message);
            this.code = code;
        }

        public String code() { return code; }
    }
}
