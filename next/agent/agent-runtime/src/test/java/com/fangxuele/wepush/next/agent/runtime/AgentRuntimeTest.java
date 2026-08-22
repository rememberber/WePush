package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.AgentId;
import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import com.fangxuele.wepush.next.core.api.ExecutionEngine;
import com.fangxuele.wepush.next.core.api.ExecutionPorts;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.core.api.RunHandle;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentRuntimeTest {
    private static final Instant NOW = Instant.parse("2026-08-22T10:00:00Z");

    @Test
    void detectsDuplicateAndGapWithoutAdvancingTheJournal() {
        InMemoryAgentJournal journal = new InMemoryAgentJournal();
        AgentRuntime runtime = runtime(journal);
        AgentFrames.Welcome welcome = new AgentFrames.Welcome(1, NOW, 10, 1024, 0);

        assertEquals(InboundSequenceResult.ACCEPTED,
                runtime.accept(new AgentFrames.ServiceToAgent(1, welcome)));
        assertEquals(InboundSequenceResult.DUPLICATE,
                runtime.accept(new AgentFrames.ServiceToAgent(1, welcome)));
        assertEquals(InboundSequenceResult.GAP,
                runtime.accept(new AgentFrames.ServiceToAgent(3, welcome)));
        assertEquals(1, journal.load().lastServiceSequence());
    }

    @Test
    void rejectsAnOlderEpochAfterAReplacementLeaseWasAccepted() {
        AgentRuntime runtime = runtime(new InMemoryAgentJournal());
        LeaseFence current = new LeaseFence("lease-1", "run-1", 2, "current-token");
        AgentFrames.LeaseOffer offer = new AgentFrames.LeaseOffer(
                current,
                NOW.plusSeconds(30),
                "https://service/spec",
                "spec-sha",
                "https://service/audience",
                "audience-sha",
                new byte[]{1, 2, 3});
        assertEquals(InboundSequenceResult.ACCEPTED,
                runtime.accept(new AgentFrames.ServiceToAgent(1, offer)));

        assertThrows(StaleLeaseException.class, () -> runtime.acknowledge(
                new LeaseFence("lease-1", "run-1", 1, "old-token")));
        assertEquals(AgentFrames.LeaseAck.class, runtime.acknowledge(current).payload().getClass());
    }

    @Test
    void resumesOutgoingSequenceFromTheJournal() {
        InMemoryAgentJournal journal = new InMemoryAgentJournal();
        AgentRuntime first = runtime(journal);
        assertEquals(1, first.hello(List.of()).sequence());
        assertEquals(2, first.heartbeat().sequence());

        AgentRuntime recovered = runtime(journal);
        assertEquals(3, recovered.heartbeat().sequence());
    }

    private static AgentRuntime runtime(InMemoryAgentJournal journal) {
        return new AgentRuntime(
                new AgentId("agent-test"),
                "0.1.0",
                2,
                new NoOpEngine(),
                journal,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static final class NoOpEngine implements ExecutionEngine {
        @Override
        public RunHandle start(RunExecutionSpec spec, ExecutionPorts ports) {
            throw new UnsupportedOperationException("not needed by this protocol test");
        }

        @Override
        public void close() {
        }
    }
}
