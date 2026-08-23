package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.LeaseFence;

import java.util.List;

/**
 * Durable Agent business-event queue. Event sequences belong to a Lease and are
 * deliberately independent from reconnect-scoped Agent frame sequences.
 */
public interface AgentEventOutbox {
    PendingBatch append(LeaseFence fence, List<byte[]> events);

    List<PendingBatch> pending();

    void acknowledge(AgentFrames.EventAck acknowledgement);

    long sizeBytes();

    record PendingBatch(LeaseFence fence, long firstEventSequence, List<byte[]> events) {
        public PendingBatch {
            if (fence == null || firstEventSequence < 1 || events == null || events.isEmpty()) {
                throw new IllegalArgumentException("pending event batch values are invalid");
            }
            events = events.stream().map(byte[]::clone).toList();
            if (events.stream().anyMatch(value -> value.length == 0)) {
                throw new IllegalArgumentException("pending events must not be empty");
            }
        }

        @Override
        public List<byte[]> events() {
            return events.stream().map(byte[]::clone).toList();
        }

        public long lastEventSequence() {
            return firstEventSequence + events.size() - 1L;
        }
    }
}
