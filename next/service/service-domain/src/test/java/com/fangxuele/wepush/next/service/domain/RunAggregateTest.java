package com.fangxuele.wepush.next.service.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RunAggregateTest {
    @Test
    void acceptsOnlyDeclaredTransitionsAndIncrementsVersion() {
        Instant created = Instant.parse("2026-08-22T00:00:00Z");
        RunAggregate run = RunAggregate.pending(new WorkspaceId("workspace-1"), "run-1", created);

        run.transitionTo(RunStatus.LEASED, created.plusSeconds(1));
        run.transitionTo(RunStatus.RUNNING, created.plusSeconds(2));
        run.transitionTo(RunStatus.SUCCEEDED, created.plusSeconds(3));

        assertEquals(RunStatus.SUCCEEDED, run.status());
        assertEquals(3, run.version());
        assertThrows(IllegalStateException.class,
                () -> run.transitionTo(RunStatus.RUNNING, created.plusSeconds(4)));
    }
}
