package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
