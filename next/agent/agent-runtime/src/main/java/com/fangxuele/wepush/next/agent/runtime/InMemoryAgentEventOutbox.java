package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.LeaseFence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryAgentEventOutbox implements AgentEventOutbox {
    private final List<PendingBatch> batches = new ArrayList<>();
    private final Map<String, Long> nextSequences = new HashMap<>();
    private long sizeBytes;

    @Override
    public synchronized PendingBatch append(LeaseFence fence, List<byte[]> events) {
        long first = nextSequences.getOrDefault(fence.leaseId(), 1L);
        PendingBatch batch = new PendingBatch(fence, first, events);
        batches.add(batch);
        nextSequences.put(fence.leaseId(), batch.lastEventSequence() + 1L);
        sizeBytes += payloadBytes(batch);
        return batch;
    }

    @Override
    public synchronized List<PendingBatch> pending() {
        return List.copyOf(batches);
    }

    @Override
    public synchronized void acknowledge(AgentFrames.EventAck acknowledgement) {
        LeaseFence fence = acknowledgement.fence();
        batches.removeIf(batch -> {
            boolean acknowledged = batch.fence().equals(fence)
                    && batch.lastEventSequence() <= acknowledgement.lastEventSequence();
            if (acknowledged) sizeBytes -= payloadBytes(batch);
            return acknowledged;
        });
    }

    @Override
    public synchronized long sizeBytes() {
        return sizeBytes;
    }

    private static long payloadBytes(PendingBatch batch) {
        return batch.events().stream().mapToLong(value -> value.length).sum();
    }
}
