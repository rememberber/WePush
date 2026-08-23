package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.LeaseFence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryAgentCompletionOutbox implements AgentCompletionOutbox {
    private final Map<LeaseFence, PendingCompletion> pending = new LinkedHashMap<>();

    @Override
    public synchronized void put(LeaseFence fence, byte[] summary, List<String> artifactReferences) {
        pending.putIfAbsent(fence, new PendingCompletion(fence, summary, artifactReferences));
    }

    @Override
    public synchronized List<PendingCompletion> pending() {
        return List.copyOf(pending.values());
    }

    @Override
    public synchronized void acknowledge(LeaseFence fence) {
        pending.remove(fence);
    }
}
