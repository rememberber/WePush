package com.fangxuele.wepush.next.agent.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaseFenceTest {
    @Test
    void authorityRequiresEpochAndOpaqueTokenToMatch() {
        LeaseFence current = new LeaseFence("lease-1", "run-1", 2, "new-token");

        assertTrue(current.sameAuthority(new LeaseFence("lease-1", "run-1", 2, "new-token")));
        assertFalse(current.sameAuthority(new LeaseFence("lease-1", "run-1", 1, "old-token")));
        assertFalse(current.toString().contains("new-token"));
    }
}
