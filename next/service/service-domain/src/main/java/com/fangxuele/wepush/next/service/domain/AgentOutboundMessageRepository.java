package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.List;

public interface AgentOutboundMessageRepository {
    boolean create(AgentOutboundMessage message);

    List<AgentOutboundMessage> pending(Instant now, int limit);

    List<AgentOutboundMessage> pendingForAgent(String agentId, Instant now, int limit);

    void delivered(String id, Instant deliveredAt, Instant nextAttemptAt);

    void failed(String id, String error, Instant nextAttemptAt);

    void acknowledgeLease(String leaseId, Instant acknowledgedAt);

    void acknowledgeCommand(String commandId, Instant acknowledgedAt);

    void discardLease(String leaseId, String reason, Instant discardedAt);
}
