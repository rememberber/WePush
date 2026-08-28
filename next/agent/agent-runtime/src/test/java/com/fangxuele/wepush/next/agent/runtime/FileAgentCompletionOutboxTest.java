package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileAgentCompletionOutboxTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void keepsCompletionAcrossRestartUntilExplicitAck() {
        Path path = temporaryDirectory.resolve("completion-outbox.bin");
        LeaseFence fence = new LeaseFence("lease-1", "run-1", 1, "token");
        FileAgentCompletionOutbox outbox = new FileAgentCompletionOutbox(path);
        outbox.put(fence, "summary".getBytes(StandardCharsets.UTF_8), List.of("artifact-1"));

        FileAgentCompletionOutbox recovered = new FileAgentCompletionOutbox(path);
        assertEquals(1, recovered.pending().size());
        assertArrayEquals("summary".getBytes(StandardCharsets.UTF_8),
                recovered.pending().getFirst().summary());
        recovered.acknowledge(fence);

        assertEquals(List.of(), new FileAgentCompletionOutbox(path).pending());
    }

    @Test
    void writeFailureDoesNotCommitPutOrAcknowledgementInMemory() throws Exception {
        Path path = temporaryDirectory.resolve("transactional.bin");
        Path temporaryPath = temporaryDirectory.resolve("transactional.bin.tmp");
        LeaseFence first = new LeaseFence("lease-1", "run-1", 1, "token-1");
        LeaseFence second = new LeaseFence("lease-2", "run-2", 1, "token-2");
        FileAgentCompletionOutbox outbox = new FileAgentCompletionOutbox(path);
        outbox.put(first, "first".getBytes(StandardCharsets.UTF_8), List.of());

        Files.createDirectories(temporaryPath);
        Files.writeString(temporaryPath.resolve("block-delete"), "simulate a full or unwritable disk");
        assertThrows(IllegalStateException.class,
                () -> outbox.put(second, "second".getBytes(StandardCharsets.UTF_8), List.of()));
        assertEquals(List.of(first), outbox.pending().stream().map(value -> value.fence()).toList());
        assertThrows(IllegalStateException.class, () -> outbox.acknowledge(first));
        assertEquals(List.of(first), outbox.pending().stream().map(value -> value.fence()).toList());

        Files.delete(temporaryPath.resolve("block-delete"));
        Files.delete(temporaryPath);
        outbox.put(second, "second".getBytes(StandardCharsets.UTF_8), List.of());
        assertEquals(List.of(first, second), new FileAgentCompletionOutbox(path).pending().stream()
                .map(value -> value.fence()).toList());
    }
}
