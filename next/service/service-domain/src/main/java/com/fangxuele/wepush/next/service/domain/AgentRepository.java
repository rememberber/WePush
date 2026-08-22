package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AgentRepository {
    Optional<AgentRegistration> findById(String agentId);

    List<AgentRegistration> list();

    void connect(AgentRegistration registration);

    void heartbeat(String agentId, String sessionId, AgentRegistration.Status status,
                   int activeRuns, int availableRuns, long lastAgentSequence, Instant lastSeenAt);

    void advanceSequence(String agentId, String sessionId, long lastAgentSequence, Instant lastSeenAt);

    void advanceServiceSequence(String agentId, String sessionId, long lastServiceSequence);

    void disconnect(String agentId, String sessionId, Instant disconnectedAt);

    int expireSilent(Instant lastSeenBefore, Instant disconnectedAt);
}
