package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileAgentEventOutboxTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsMonotonicLeaseSequencesAndDeletesOnlyAcknowledgedFence() {
        Path path = temporaryDirectory.resolve("event-outbox.bin");
        LeaseFence firstFence = new LeaseFence("lease-1", "run-1", 1, "fence-1");
        LeaseFence secondFence = new LeaseFence("lease-2", "run-2", 1, "fence-2");
        FileAgentEventOutbox outbox = new FileAgentEventOutbox(path);

        assertEquals(1, outbox.append(firstFence, List.of(bytes("one"))).firstEventSequence());
        assertEquals(2, outbox.append(firstFence, List.of(bytes("two"), bytes("three")))
                .firstEventSequence());
        outbox.append(secondFence, List.of(bytes("other")));

        FileAgentEventOutbox recovered = new FileAgentEventOutbox(path);
        assertEquals(3, recovered.pending().size());
        assertArrayEquals(bytes("one"), recovered.pending().getFirst().events().getFirst());
        recovered.acknowledge(new AgentFrames.EventAck(firstFence, 1));

        FileAgentEventOutbox afterAck = new FileAgentEventOutbox(path);
        assertEquals(2, afterAck.pending().size());
        assertEquals(4, afterAck.append(firstFence, List.of(bytes("four"))).firstEventSequence());
        assertEquals(1, afterAck.pending().stream()
                .filter(batch -> batch.fence().equals(secondFence)).count());
    }

    @Test
    void rejectsAppendBeforeExceedingDiskBudgetAndKeepsExistingData() {
        Path path = temporaryDirectory.resolve("bounded.bin");
        LeaseFence fence = new LeaseFence("lease-1", "run-1", 1, "fence-1");
        FileAgentEventOutbox outbox = new FileAgentEventOutbox(path, 13);
        outbox.append(fence, List.of(bytes("1234")));

        assertThrows(FileAgentEventOutbox.EventOutboxFullException.class,
                () -> outbox.append(fence, List.of(bytes("12345"))));

        FileAgentEventOutbox recovered = new FileAgentEventOutbox(path, 13);
        assertEquals(1, recovered.pending().size());
        assertEquals(2, recovered.append(fence, List.of(bytes("1"))).firstEventSequence());
    }

    @Test
    void writeFailureDoesNotCommitAppendOrAcknowledgementInMemory() throws Exception {
        Path path = temporaryDirectory.resolve("transactional.bin");
        Path temporaryPath = temporaryDirectory.resolve("transactional.bin.tmp");
        LeaseFence fence = new LeaseFence("lease-1", "run-1", 1, "fence-1");
        FileAgentEventOutbox outbox = new FileAgentEventOutbox(path);
        outbox.append(fence, List.of(bytes("one")));

        Files.createDirectories(temporaryPath);
        Files.writeString(temporaryPath.resolve("block-delete"), "simulate a full or unwritable disk");
        assertThrows(IllegalStateException.class,
                () -> outbox.append(fence, List.of(bytes("two"))));
        assertEquals(1, outbox.pending().size());
        assertEquals(7, outbox.sizeBytes());
        assertThrows(IllegalStateException.class,
                () -> outbox.acknowledge(new AgentFrames.EventAck(fence, 1)));
        assertEquals(1, outbox.pending().size());

        Files.delete(temporaryPath.resolve("block-delete"));
        Files.delete(temporaryPath);
        assertEquals(2, outbox.append(fence, List.of(bytes("two"))).firstEventSequence());
        assertEquals(2, new FileAgentEventOutbox(path).pending().size());
    }

    private static byte[] bytes(String value) {
        return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
