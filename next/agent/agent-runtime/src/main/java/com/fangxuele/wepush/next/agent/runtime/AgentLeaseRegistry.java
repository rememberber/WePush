package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.LeaseFence;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

final class AgentLeaseRegistry {
    private final Map<String, AgentJournalState.PersistedLease> leases;

    AgentLeaseRegistry(Map<String, AgentJournalState.PersistedLease> recovered) {
        this.leases = new HashMap<>(recovered);
    }

    synchronized boolean offer(AgentFrames.LeaseOffer offer, Instant now) {
        if (!offer.expiresAt().isAfter(now)) {
            throw new StaleLeaseException("lease offer is already expired");
        }
        AgentJournalState.PersistedLease current = leases.get(offer.fence().leaseId());
        if (current != null) {
            if (offer.fence().epoch() < current.fence().epoch()) {
                throw new StaleLeaseException("lease epoch is older than the local journal");
            }
            if (offer.fence().epoch() == current.fence().epoch()
                    && !current.fence().sameAuthority(offer.fence())) {
                throw new StaleLeaseException("same lease epoch has a different fencing token");
            }
            if (current.fence().sameAuthority(offer.fence())) {
                return false;
            }
        }
        leases.put(offer.fence().leaseId(), new AgentJournalState.PersistedLease(
                offer.fence(), offer.expiresAt(), LeaseState.OFFERED));
        return true;
    }

    synchronized void prepared(LeaseFence fence, String executionSpecSha256, String audienceSha256,
                               long totalRecipients, Instant startedAt, Instant now) {
        AgentJournalState.PersistedLease lease = requireAuthority(fence, now);
        if (lease.state() != LeaseState.OFFERED) {
            throw new IllegalStateException("lease can only be prepared before acknowledgement");
        }
        leases.put(fence.leaseId(), new AgentJournalState.PersistedLease(fence, lease.expiresAt(),
                lease.state(), executionSpecSha256, audienceSha256, totalRecipients, startedAt));
    }

    synchronized void acknowledge(LeaseFence fence, Instant now) {
        AgentJournalState.PersistedLease lease = requireAuthority(fence, now);
        if (lease.state() != LeaseState.OFFERED && lease.state() != LeaseState.ACKNOWLEDGED) {
            throw new IllegalStateException("lease cannot be acknowledged from " + lease.state());
        }
        transition(lease, LeaseState.ACKNOWLEDGED);
    }

    synchronized void running(LeaseFence fence, Instant now) {
        AgentJournalState.PersistedLease lease = requireAuthority(fence, now);
        if (lease.state() != LeaseState.ACKNOWLEDGED) {
            throw new IllegalStateException("lease must be acknowledged before execution");
        }
        transition(lease, LeaseState.RUNNING);
    }

    synchronized void complete(LeaseFence fence) {
        AgentJournalState.PersistedLease lease = requireAuthority(fence, Instant.MIN);
        if (lease.state() != LeaseState.RUNNING) {
            throw new IllegalStateException("only a running lease can complete");
        }
        transition(lease, LeaseState.COMPLETED);
    }

    synchronized void completeRecovered(LeaseFence fence) {
        AgentJournalState.PersistedLease lease = requireAuthority(fence, Instant.MIN);
        if (lease.state() == LeaseState.COMPLETED) return;
        if (lease.state() != LeaseState.RUNNING && lease.state() != LeaseState.ACKNOWLEDGED) {
            throw new IllegalStateException("lease is not awaiting process recovery completion");
        }
        transition(lease, LeaseState.COMPLETED);
    }

    synchronized void release(LeaseFence fence) {
        AgentJournalState.PersistedLease lease = requireAuthority(fence, Instant.MIN);
        if (lease.state() != LeaseState.RUNNING) {
            throw new IllegalStateException("only a running lease can be released");
        }
        transition(lease, LeaseState.RELEASED);
    }

    synchronized void validateCommand(LeaseFence fence, Instant now) {
        AgentJournalState.PersistedLease lease = requireAuthority(fence, now);
        if (lease.state() != LeaseState.RUNNING) {
            throw new StaleLeaseException("command does not target a running lease");
        }
    }

    synchronized int runningCount() {
        return (int) leases.values().stream().filter(lease -> lease.state() == LeaseState.RUNNING).count();
    }

    synchronized Map<String, AgentJournalState.PersistedLease> snapshot() {
        return Map.copyOf(leases);
    }

    private AgentJournalState.PersistedLease requireAuthority(LeaseFence fence, Instant now) {
        AgentJournalState.PersistedLease current = leases.get(fence.leaseId());
        if (current == null || !current.fence().sameAuthority(fence)) {
            throw new StaleLeaseException("lease fencing authority is stale or unknown");
        }
        if (!now.equals(Instant.MIN) && current.state() != LeaseState.RUNNING
                && !current.expiresAt().isAfter(now)) {
            throw new StaleLeaseException("lease has expired");
        }
        return current;
    }

    private void transition(AgentJournalState.PersistedLease lease, LeaseState state) {
        leases.put(lease.fence().leaseId(), new AgentJournalState.PersistedLease(
                lease.fence(), lease.expiresAt(), state, lease.executionSpecSha256(),
                lease.audienceSha256(), lease.totalRecipients(), lease.executionStartedAt()));
    }
}
