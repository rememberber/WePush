package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.AgentId;
import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import com.fangxuele.wepush.next.agent.protocol.ProviderCapability;
import com.fangxuele.wepush.next.core.api.ExecutionEngine;
import com.fangxuele.wepush.next.core.api.ExecutionPorts;
import com.fangxuele.wepush.next.core.api.RunCommand;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.core.api.RunHandle;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class AgentRuntime implements AutoCloseable {
    private final AgentId agentId;
    private final String agentVersion;
    private final int maximumRuns;
    private final ExecutionEngine engine;
    private final AgentJournal journal;
    private final Clock clock;
    private final AgentLeaseRegistry leases;
    private final Map<String, RunHandle> activeRuns = new ConcurrentHashMap<>();
    private long lastAgentSequence;
    private long lastServiceSequence;

    public AgentRuntime(
            AgentId agentId,
            String agentVersion,
            int maximumRuns,
            ExecutionEngine engine,
            AgentJournal journal
    ) {
        this(agentId, agentVersion, maximumRuns, engine, journal, Clock.systemUTC());
    }

    AgentRuntime(
            AgentId agentId,
            String agentVersion,
            int maximumRuns,
            ExecutionEngine engine,
            AgentJournal journal,
            Clock clock
    ) {
        this.agentId = Objects.requireNonNull(agentId, "agentId");
        if (agentVersion == null || agentVersion.isBlank() || maximumRuns < 1) {
            throw new IllegalArgumentException("agent version and capacity are required");
        }
        this.agentVersion = agentVersion;
        this.maximumRuns = maximumRuns;
        this.engine = Objects.requireNonNull(engine, "engine");
        this.journal = Objects.requireNonNull(journal, "journal");
        this.clock = Objects.requireNonNull(clock, "clock");
        AgentJournalState recovered = journal.load();
        this.lastAgentSequence = recovered.lastAgentSequence();
        this.lastServiceSequence = recovered.lastServiceSequence();
        this.leases = new AgentLeaseRegistry(recovered.leases());
    }

    public synchronized AgentFrames.AgentToService hello(List<ProviderCapability> providers) {
        return hello(providers, "");
    }

    public synchronized AgentFrames.AgentToService hello(
            List<ProviderCapability> providers, String secretEncryptionPublicKey) {
        AgentFrames.Hello hello = new AgentFrames.Hello(
                agentVersion,
                1,
                1,
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                Runtime.version().feature() + "",
                maximumRuns,
                lastServiceSequence,
                lastAgentSequence,
                providers,
                secretEncryptionPublicKey);
        return next(hello);
    }

    public synchronized InboundSequenceResult accept(AgentFrames.ServiceToAgent frame) {
        Objects.requireNonNull(frame, "frame");
        if (frame.sequence() <= lastServiceSequence) {
            return InboundSequenceResult.DUPLICATE;
        }
        if (frame.sequence() != lastServiceSequence + 1) {
            return InboundSequenceResult.GAP;
        }
        if (frame.payload() instanceof AgentFrames.LeaseOffer offer) {
            leases.offer(offer, clock.instant());
        }
        lastServiceSequence = frame.sequence();
        persist();
        return InboundSequenceResult.ACCEPTED;
    }

    public synchronized AgentFrames.AgentToService acknowledge(LeaseFence fence) {
        leases.acknowledge(fence, clock.instant());
        persist();
        return next(new AgentFrames.LeaseAck(fence));
    }

    public RunHandle start(LeaseFence fence, RunExecutionSpec spec, ExecutionPorts ports) {
        Objects.requireNonNull(spec, "spec");
        if (!fence.runId().equals(spec.runId())) {
            throw new StaleLeaseException("lease run and execution specification do not match");
        }
        synchronized (this) {
            if (leases.runningCount() >= maximumRuns) {
                throw new IllegalStateException("agent run capacity is exhausted");
            }
            leases.running(fence, clock.instant());
            persist();
        }
        RunHandle handle;
        try {
            handle = engine.start(spec, ports);
        } catch (RuntimeException error) {
            synchronized (this) {
                leases.release(fence);
                persist();
            }
            throw error;
        }
        activeRuns.put(spec.runId(), handle);
        handle.completion().whenComplete((_summary, _error) -> {
            activeRuns.remove(spec.runId(), handle);
            synchronized (AgentRuntime.this) {
                leases.complete(fence);
                persist();
            }
        });
        return handle;
    }

    public synchronized com.fangxuele.wepush.next.core.api.CommandResult command(
            LeaseFence fence,
            RunCommand command
    ) {
        leases.validateCommand(fence, clock.instant());
        RunHandle handle = activeRuns.get(fence.runId());
        if (handle == null) {
            throw new StaleLeaseException("run is not active on this agent");
        }
        return handle.submit(command);
    }

    public synchronized AgentFrames.AgentToService heartbeat() {
        int active = activeRuns.size();
        List<LeaseFence> currentLeases = leases.snapshot().values().stream()
                .filter(lease -> lease.state() == LeaseState.RUNNING || lease.state() == LeaseState.ACKNOWLEDGED)
                .map(AgentJournalState.PersistedLease::fence)
                .toList();
        return next(new AgentFrames.Heartbeat(
                "READY",
                active,
                Math.max(0, maximumRuns - active),
                currentLeases));
    }

    public synchronized AgentFrames.AgentToService events(
            LeaseFence fence, long firstEventSequence, List<byte[]> events) {
        return next(new AgentFrames.EventBatch(fence, firstEventSequence, events));
    }

    public synchronized AgentFrames.AgentToService commandAcknowledged(
            String commandId, LeaseFence fence, String outcome, String detail) {
        return next(new AgentFrames.CommandAck(commandId, fence, outcome, detail));
    }

    public synchronized AgentFrames.AgentToService completed(
            LeaseFence fence, byte[] summary, List<String> artifactReferences) {
        return next(new AgentFrames.RunCompleted(fence, summary, artifactReferences));
    }

    public synchronized AgentJournalState journalState() {
        return new AgentJournalState(lastAgentSequence, lastServiceSequence, leases.snapshot());
    }

    private AgentFrames.AgentToService next(AgentFrames.AgentPayload payload) {
        lastAgentSequence++;
        persist();
        return new AgentFrames.AgentToService(agentId, lastAgentSequence, payload);
    }

    private void persist() {
        journal.save(new AgentJournalState(lastAgentSequence, lastServiceSequence, leases.snapshot()));
    }

    @Override
    public void close() {
        engine.close();
    }
}
