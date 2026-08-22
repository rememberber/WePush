package com.fangxuele.wepush.next.agent.runtime;

import com.fangxuele.wepush.next.agent.protocol.LeaseFence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileAgentJournalTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void atomicallyPersistsSequencesAndLeaseFences() {
        FileAgentJournal journal = new FileAgentJournal(temporaryDirectory.resolve("agent.properties"));
        LeaseFence fence = new LeaseFence("lease_1", "run_1", 3, "fence-value");
        AgentJournalState expected = new AgentJournalState(12, 9, Map.of("lease_1",
                new AgentJournalState.PersistedLease(fence,
                        Instant.parse("2026-08-22T12:00:00Z"), LeaseState.ACKNOWLEDGED)));

        journal.save(expected);

        assertEquals(expected, journal.load());
    }
}
