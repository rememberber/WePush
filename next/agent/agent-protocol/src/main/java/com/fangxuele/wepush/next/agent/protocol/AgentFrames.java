package com.fangxuele.wepush.next.agent.protocol;

import java.time.Instant;
import java.util.List;

public final class AgentFrames {
    private AgentFrames() {
    }

    public record AgentToService(AgentId agentId, long sequence, AgentPayload payload) {
        public AgentToService {
            if (agentId == null || payload == null || sequence < 1) {
                throw new IllegalArgumentException("agent frame requires identity, payload, and positive sequence");
            }
        }
    }

    public record ServiceToAgent(long sequence, ServicePayload payload) {
        public ServiceToAgent {
            if (payload == null || sequence < 1) {
                throw new IllegalArgumentException("service frame requires payload and positive sequence");
            }
        }
    }

    public sealed interface AgentPayload permits Hello, Heartbeat, LeaseAck, EventBatch,
            CommandAck, RunCompleted, Draining {
    }

    public sealed interface ServicePayload permits Welcome, LeaseOffer, RunCommand, EventAck, DrainRequest {
    }

    public record Hello(
            String agentVersion,
            int protocolMinimum,
            int protocolMaximum,
            String operatingSystem,
            String architecture,
            String javaVersion,
            int maximumRuns,
            long lastServiceSequence,
            long lastAgentSequenceAcknowledged,
            List<ProviderCapability> providers
    ) implements AgentPayload {
        public Hello {
            if (protocolMinimum < 1 || protocolMaximum < protocolMinimum || maximumRuns < 1) {
                throw new IllegalArgumentException("invalid hello protocol range or capacity");
            }
            providers = List.copyOf(providers);
        }
    }

    public record Welcome(
            int protocolVersion,
            Instant serverTime,
            int heartbeatSeconds,
            long maximumMessageBytes,
            long lastAgentSequence
    ) implements ServicePayload {
        public Welcome {
            if (protocolVersion < 1 || serverTime == null || heartbeatSeconds < 1 || maximumMessageBytes < 1) {
                throw new IllegalArgumentException("invalid welcome values");
            }
        }
    }

    public record Heartbeat(String state, int activeRuns, int availableRuns, List<LeaseFence> leases)
            implements AgentPayload {
        public Heartbeat {
            if (state == null || state.isBlank() || activeRuns < 0 || availableRuns < 0) {
                throw new IllegalArgumentException("invalid heartbeat values");
            }
            leases = List.copyOf(leases);
        }
    }

    public record LeaseOffer(
            LeaseFence fence,
            Instant expiresAt,
            String executionSpecUrl,
            String executionSpecSha256,
            String audienceUrl,
            String audienceSha256,
            byte[] secretEnvelope
    ) implements ServicePayload {
        public LeaseOffer {
            if (fence == null || expiresAt == null || executionSpecUrl == null || audienceUrl == null) {
                throw new IllegalArgumentException("lease offer must include fence, expiry, and artifact URLs");
            }
            secretEnvelope = secretEnvelope == null ? new byte[0] : secretEnvelope.clone();
        }

        @Override
        public byte[] secretEnvelope() {
            return secretEnvelope.clone();
        }
    }

    public record LeaseAck(LeaseFence fence) implements AgentPayload {
        public LeaseAck {
            if (fence == null) {
                throw new IllegalArgumentException("lease acknowledgement requires a fence");
            }
        }
    }

    public record EventBatch(LeaseFence fence, long firstEventSequence, List<byte[]> events)
            implements AgentPayload {
        public EventBatch {
            if (fence == null || firstEventSequence < 1 || events == null) {
                throw new IllegalArgumentException("event batch requires a fence, events, and positive sequence");
            }
            events = events.stream().map(byte[]::clone).toList();
        }

        @Override
        public List<byte[]> events() {
            return events.stream().map(byte[]::clone).toList();
        }
    }

    public record RunCommand(String commandId, LeaseFence fence, String type, byte[] payload, Instant createdAt)
            implements ServicePayload {
        public RunCommand {
            if (commandId == null || commandId.isBlank() || fence == null
                    || type == null || type.isBlank() || createdAt == null) {
                throw new IllegalArgumentException("run command requires identity, fence, type, and creation time");
            }
            payload = payload == null ? new byte[0] : payload.clone();
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }
    }

    public record CommandAck(String commandId, LeaseFence fence, String outcome, String detail)
            implements AgentPayload {
        public CommandAck {
            if (commandId == null || commandId.isBlank() || fence == null
                    || outcome == null || outcome.isBlank()) {
                throw new IllegalArgumentException("command acknowledgement values are incomplete");
            }
            detail = detail == null ? "" : detail;
        }
    }

    public record EventAck(LeaseFence fence, long lastEventSequence) implements ServicePayload {
        public EventAck {
            if (fence == null || lastEventSequence < 1) {
                throw new IllegalArgumentException("event acknowledgement requires a fence and positive sequence");
            }
        }
    }

    public record RunCompleted(LeaseFence fence, byte[] summary, List<String> artifactReferences)
            implements AgentPayload {
        public RunCompleted {
            if (fence == null || artifactReferences == null) {
                throw new IllegalArgumentException("run completion requires a fence and artifact references");
            }
            summary = summary == null ? new byte[0] : summary.clone();
            artifactReferences = List.copyOf(artifactReferences);
        }

        @Override
        public byte[] summary() {
            return summary.clone();
        }
    }

    public record DrainRequest(Instant deadline) implements ServicePayload {
        public DrainRequest {
            if (deadline == null) {
                throw new IllegalArgumentException("drain request requires a deadline");
            }
        }
    }

    public record Draining(int activeRuns) implements AgentPayload {
        public Draining {
            if (activeRuns < 0) {
                throw new IllegalArgumentException("activeRuns must not be negative");
            }
        }
    }
}
