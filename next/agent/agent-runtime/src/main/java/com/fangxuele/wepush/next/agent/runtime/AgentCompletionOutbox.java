package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.LeaseFence;

import java.util.List;

public interface AgentCompletionOutbox {
    void put(LeaseFence fence, byte[] summary, List<String> artifactReferences);

    List<PendingCompletion> pending();

    void acknowledge(LeaseFence fence);

    record PendingCompletion(LeaseFence fence, byte[] summary, List<String> artifactReferences) {
        public PendingCompletion {
            if (fence == null || summary == null || summary.length == 0 || artifactReferences == null) {
                throw new IllegalArgumentException("pending completion is incomplete");
            }
            summary = summary.clone();
            artifactReferences = List.copyOf(artifactReferences);
        }

        @Override
        public byte[] summary() {
            return summary.clone();
        }
    }
}
