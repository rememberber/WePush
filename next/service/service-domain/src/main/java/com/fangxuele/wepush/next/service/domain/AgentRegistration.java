package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.List;

public record AgentRegistration(
        String id,
        Status status,
        String agentVersion,
        int protocolVersion,
        String operatingSystem,
        String architecture,
        String javaVersion,
        int maximumRuns,
        int activeRuns,
        int availableRuns,
        List<Provider> providers,
        String secretEncryptionPublicKey,
        String sessionId,
        long lastAgentSequence,
        long lastServiceSequence,
        Instant connectedAt,
        Instant lastSeenAt,
        Instant disconnectedAt,
        long version
) {
    public AgentRegistration {
        id = DomainChecks.text(id, "agent id");
        agentVersion = DomainChecks.text(agentVersion, "agent version");
        operatingSystem = DomainChecks.text(operatingSystem, "agent operating system");
        architecture = DomainChecks.text(architecture, "agent architecture");
        javaVersion = DomainChecks.text(javaVersion, "agent java version");
        sessionId = DomainChecks.text(sessionId, "agent session id");
        providers = List.copyOf(providers);
        secretEncryptionPublicKey = secretEncryptionPublicKey == null
                ? "" : secretEncryptionPublicKey.trim();
        if (secretEncryptionPublicKey.length() > 512) {
            throw new IllegalArgumentException("agent secret encryption public key is too large");
        }
        if (status == null || protocolVersion < 1 || maximumRuns < 1 || activeRuns < 0
                || availableRuns < 0 || activeRuns + availableRuns > maximumRuns
                || lastAgentSequence < 1 || lastServiceSequence < 1
                || connectedAt == null || lastSeenAt == null || lastSeenAt.isBefore(connectedAt)
                || version < 0) {
            throw new IllegalArgumentException("agent registration is incomplete");
        }
    }

    public AgentRegistration(String id, Status status, String agentVersion, int protocolVersion,
                             String operatingSystem, String architecture, String javaVersion,
                             int maximumRuns, int activeRuns, int availableRuns,
                             List<Provider> providers, String sessionId,
                             long lastAgentSequence, long lastServiceSequence,
                             Instant connectedAt, Instant lastSeenAt, Instant disconnectedAt,
                             long version) {
        this(id, status, agentVersion, protocolVersion, operatingSystem, architecture, javaVersion,
                maximumRuns, activeRuns, availableRuns, providers, "", sessionId,
                lastAgentSequence, lastServiceSequence, connectedAt, lastSeenAt,
                disconnectedAt, version);
    }

    public enum Status { ONLINE, DRAINING, DEGRADED, OFFLINE }

    public record Provider(String providerId, String implementationVersion,
                           int spiMajor, int maximumConcurrency) {
        public Provider {
            providerId = DomainChecks.text(providerId, "agent provider id");
            implementationVersion = DomainChecks.text(implementationVersion, "agent provider version");
            if (spiMajor < 1 || maximumConcurrency < 1) {
                throw new IllegalArgumentException("agent provider capability is invalid");
            }
        }
    }
}
