package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.LeaseFence;

import java.time.Instant;
import java.util.Map;

public record AgentJournalState(
        long lastAgentSequence,
        long lastServiceSequence,
        Map<String, PersistedLease> leases
) {
    public AgentJournalState {
        if (lastAgentSequence < 0 || lastServiceSequence < 0) {
            throw new IllegalArgumentException("sequences must not be negative");
        }
        leases = Map.copyOf(leases);
    }

    public static AgentJournalState empty() {
        return new AgentJournalState(0, 0, Map.of());
    }

    public record PersistedLease(LeaseFence fence, Instant expiresAt, LeaseState state) {
        public PersistedLease {
            if (fence == null || expiresAt == null || state == null) {
                throw new IllegalArgumentException("persisted lease values must not be null");
            }
        }
    }
}
