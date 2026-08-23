package com.fangxuele.wepush.next.agent.protocol;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import java.time.Instant;

public final class AgentProtoMapper {
    private AgentProtoMapper() {
    }

    public static com.fangxuele.wepush.next.agent.protocol.v1.AgentToService toProto(
            AgentFrames.AgentToService frame) {
        var builder = com.fangxuele.wepush.next.agent.protocol.v1.AgentToService.newBuilder()
                .setAgentId(frame.agentId().value()).setSequence(frame.sequence());
        switch (frame.payload()) {
            case AgentFrames.Hello value -> builder.setHello(toProto(value));
            case AgentFrames.Heartbeat value -> builder.setHeartbeat(toProto(value));
            case AgentFrames.LeaseAck value -> builder.setLeaseAck(
                    com.fangxuele.wepush.next.agent.protocol.v1.LeaseAck.newBuilder()
                            .setFence(toProto(value.fence())));
            case AgentFrames.EventBatch value -> {
                var payload = com.fangxuele.wepush.next.agent.protocol.v1.EventBatch.newBuilder()
                        .setFence(toProto(value.fence()))
                        .setFirstEventSequence(value.firstEventSequence());
                value.events().forEach(item -> payload.addEvents(ByteString.copyFrom(item)));
                builder.setEventBatch(payload);
            }
            case AgentFrames.CommandAck value -> builder.setCommandAck(
                    com.fangxuele.wepush.next.agent.protocol.v1.CommandAck.newBuilder()
                            .setCommandId(value.commandId()).setFence(toProto(value.fence()))
                            .setOutcome(value.outcome()).setDetail(value.detail()));
            case AgentFrames.RunCompleted value -> {
                var payload = com.fangxuele.wepush.next.agent.protocol.v1.RunCompleted.newBuilder()
                        .setFence(toProto(value.fence())).setSummary(ByteString.copyFrom(value.summary()))
                        .addAllArtifactRefs(value.artifactReferences());
                builder.setRunCompleted(payload);
            }
            case AgentFrames.Draining value -> builder.setDraining(
                    com.fangxuele.wepush.next.agent.protocol.v1.Draining.newBuilder()
                            .setActiveRuns(value.activeRuns()));
        }
        return builder.build();
    }

    public static AgentFrames.AgentToService fromProto(
            com.fangxuele.wepush.next.agent.protocol.v1.AgentToService frame) {
        AgentFrames.AgentPayload payload = switch (frame.getPayloadCase()) {
            case HELLO -> fromProto(frame.getHello());
            case HEARTBEAT -> fromProto(frame.getHeartbeat());
            case LEASE_ACK -> new AgentFrames.LeaseAck(fromProto(frame.getLeaseAck().getFence()));
            case EVENT_BATCH -> new AgentFrames.EventBatch(fromProto(frame.getEventBatch().getFence()),
                    frame.getEventBatch().getFirstEventSequence(),
                    frame.getEventBatch().getEventsList().stream().map(ByteString::toByteArray).toList());
            case COMMAND_ACK -> new AgentFrames.CommandAck(frame.getCommandAck().getCommandId(),
                    fromProto(frame.getCommandAck().getFence()), frame.getCommandAck().getOutcome(),
                    frame.getCommandAck().getDetail());
            case RUN_COMPLETED -> new AgentFrames.RunCompleted(fromProto(frame.getRunCompleted().getFence()),
                    frame.getRunCompleted().getSummary().toByteArray(),
                    frame.getRunCompleted().getArtifactRefsList());
            case DRAINING -> new AgentFrames.Draining(frame.getDraining().getActiveRuns());
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException("Agent frame payload is missing");
        };
        return new AgentFrames.AgentToService(new AgentId(frame.getAgentId()), frame.getSequence(), payload);
    }

    public static com.fangxuele.wepush.next.agent.protocol.v1.ServiceToAgent toProto(
            AgentFrames.ServiceToAgent frame) {
        var builder = com.fangxuele.wepush.next.agent.protocol.v1.ServiceToAgent.newBuilder()
                .setSequence(frame.sequence());
        switch (frame.payload()) {
            case AgentFrames.Welcome value -> builder.setWelcome(
                    com.fangxuele.wepush.next.agent.protocol.v1.Welcome.newBuilder()
                            .setProtocolVersion(value.protocolVersion()).setServerTime(toTimestamp(value.serverTime()))
                            .setHeartbeatSeconds(value.heartbeatSeconds())
                            .setMaximumMessageBytes(value.maximumMessageBytes())
                            .setLastAgentSequence(value.lastAgentSequence()));
            case AgentFrames.LeaseOffer value -> builder.setLeaseOffer(
                    com.fangxuele.wepush.next.agent.protocol.v1.LeaseOffer.newBuilder()
                            .setFence(toProto(value.fence())).setExpiresAt(toTimestamp(value.expiresAt()))
                            .setExecutionSpecUrl(value.executionSpecUrl())
                            .setExecutionSpecSha256(value.executionSpecSha256())
                            .setAudienceUrl(value.audienceUrl()).setAudienceSha256(value.audienceSha256())
                            .setSecretEnvelope(ByteString.copyFrom(value.secretEnvelope())));
            case AgentFrames.RunCommand value -> builder.setCommand(
                    com.fangxuele.wepush.next.agent.protocol.v1.RunCommand.newBuilder()
                            .setCommandId(value.commandId()).setFence(toProto(value.fence()))
                            .setType(value.type()).setPayload(ByteString.copyFrom(value.payload()))
                            .setCreatedAt(toTimestamp(value.createdAt())));
            case AgentFrames.EventAck value -> builder.setEventAck(
                    com.fangxuele.wepush.next.agent.protocol.v1.EventAck.newBuilder()
                            .setFence(toProto(value.fence())).setLastEventSequence(value.lastEventSequence()));
            case AgentFrames.DrainRequest value -> builder.setDrain(
                    com.fangxuele.wepush.next.agent.protocol.v1.DrainRequest.newBuilder()
                            .setDeadline(toTimestamp(value.deadline())));
        }
        return builder.build();
    }

    public static AgentFrames.ServiceToAgent fromProto(
            com.fangxuele.wepush.next.agent.protocol.v1.ServiceToAgent frame) {
        AgentFrames.ServicePayload payload = switch (frame.getPayloadCase()) {
            case WELCOME -> new AgentFrames.Welcome(frame.getWelcome().getProtocolVersion(),
                    fromTimestamp(frame.getWelcome().getServerTime()),
                    frame.getWelcome().getHeartbeatSeconds(), frame.getWelcome().getMaximumMessageBytes(),
                    frame.getWelcome().getLastAgentSequence());
            case LEASE_OFFER -> new AgentFrames.LeaseOffer(fromProto(frame.getLeaseOffer().getFence()),
                    fromTimestamp(frame.getLeaseOffer().getExpiresAt()),
                    frame.getLeaseOffer().getExecutionSpecUrl(), frame.getLeaseOffer().getExecutionSpecSha256(),
                    frame.getLeaseOffer().getAudienceUrl(), frame.getLeaseOffer().getAudienceSha256(),
                    frame.getLeaseOffer().getSecretEnvelope().toByteArray());
            case COMMAND -> new AgentFrames.RunCommand(frame.getCommand().getCommandId(),
                    fromProto(frame.getCommand().getFence()), frame.getCommand().getType(),
                    frame.getCommand().getPayload().toByteArray(),
                    fromTimestamp(frame.getCommand().getCreatedAt()));
            case EVENT_ACK -> new AgentFrames.EventAck(fromProto(frame.getEventAck().getFence()),
                    frame.getEventAck().getLastEventSequence());
            case DRAIN -> new AgentFrames.DrainRequest(fromTimestamp(frame.getDrain().getDeadline()));
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException("Service frame payload is missing");
        };
        return new AgentFrames.ServiceToAgent(frame.getSequence(), payload);
    }

    private static com.fangxuele.wepush.next.agent.protocol.v1.Hello toProto(AgentFrames.Hello value) {
        var builder = com.fangxuele.wepush.next.agent.protocol.v1.Hello.newBuilder()
                .setAgentVersion(value.agentVersion()).setProtocolMin(value.protocolMinimum())
                .setProtocolMax(value.protocolMaximum()).setOs(value.operatingSystem())
                .setArch(value.architecture()).setJavaVersion(value.javaVersion())
                .setMaximumRuns(value.maximumRuns()).setLastServiceSequence(value.lastServiceSequence())
                .setLastAgentSequenceAcked(value.lastAgentSequenceAcknowledged())
                .setSecretEncryptionPublicKey(value.secretEncryptionPublicKey());
        value.providers().forEach(provider -> builder.addProviders(
                com.fangxuele.wepush.next.agent.protocol.v1.ProviderCapability.newBuilder()
                        .setProviderId(provider.providerId())
                        .setImplementationVersion(provider.implementationVersion())
                        .setSpiMajor(provider.spiMajorVersion())
                        .setMaximumConcurrency(provider.maximumConcurrency())));
        return builder.build();
    }

    private static AgentFrames.Hello fromProto(
            com.fangxuele.wepush.next.agent.protocol.v1.Hello value) {
        return new AgentFrames.Hello(value.getAgentVersion(), value.getProtocolMin(), value.getProtocolMax(),
                value.getOs(), value.getArch(), value.getJavaVersion(), value.getMaximumRuns(),
                value.getLastServiceSequence(), value.getLastAgentSequenceAcked(),
                value.getProvidersList().stream().map(provider -> new ProviderCapability(
                        provider.getProviderId(), provider.getImplementationVersion(),
                        provider.getSpiMajor(), provider.getMaximumConcurrency())).toList(),
                value.getSecretEncryptionPublicKey());
    }

    private static com.fangxuele.wepush.next.agent.protocol.v1.Heartbeat toProto(
            AgentFrames.Heartbeat value) {
        var builder = com.fangxuele.wepush.next.agent.protocol.v1.Heartbeat.newBuilder()
                .setState(value.state()).setActiveRuns(value.activeRuns())
                .setAvailableRuns(value.availableRuns());
        value.leases().forEach(lease -> builder.addLeases(toProto(lease)));
        return builder.build();
    }

    private static AgentFrames.Heartbeat fromProto(
            com.fangxuele.wepush.next.agent.protocol.v1.Heartbeat value) {
        return new AgentFrames.Heartbeat(value.getState(), value.getActiveRuns(), value.getAvailableRuns(),
                value.getLeasesList().stream().map(AgentProtoMapper::fromProto).toList());
    }

    private static com.fangxuele.wepush.next.agent.protocol.v1.LeaseFence toProto(LeaseFence value) {
        return com.fangxuele.wepush.next.agent.protocol.v1.LeaseFence.newBuilder()
                .setLeaseId(value.leaseId()).setRunId(value.runId()).setEpoch(value.epoch())
                .setFencingToken(value.fencingToken()).build();
    }

    private static LeaseFence fromProto(
            com.fangxuele.wepush.next.agent.protocol.v1.LeaseFence value) {
        return new LeaseFence(value.getLeaseId(), value.getRunId(), value.getEpoch(), value.getFencingToken());
    }

    private static Timestamp toTimestamp(Instant value) {
        return Timestamp.newBuilder().setSeconds(value.getEpochSecond()).setNanos(value.getNano()).build();
    }

    private static Instant fromTimestamp(Timestamp value) {
        return Instant.ofEpochSecond(value.getSeconds(), value.getNanos());
    }
}
