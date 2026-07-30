package com.elfmcys.ysm.network.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStateReportLifecycleTest {
    @Test
    void waitsForAuthorityBeforeAllocatingTheFirstFullReport() {
        var lifecycle = new PlayerStateReportLifecycle();
        lifecycle.beginSession();

        assertEquals(PlayerStateReportLifecycle.Phase.WAIT_AUTHORITY, lifecycle.phase());
        assertThrows(IllegalStateException.class, lifecycle::allocateSequence);

        lifecycle.authorityReceived(false);
        var generation = lifecycle.generation();
        assertEquals(PlayerStateReportLifecycle.Phase.NEED_FULL, lifecycle.phase());
        assertEquals(1, lifecycle.allocateSequence());
        assertTrue(lifecycle.completeSend(generation, true));
        assertEquals(PlayerStateReportLifecycle.Phase.ACTIVE, lifecycle.phase());
    }

    @Test
    void rebindKeepsTheConnectionSequenceButRequiresAnotherFull() {
        var lifecycle = new PlayerStateReportLifecycle();
        lifecycle.beginSession();
        lifecycle.authorityReceived(false);
        var initialGeneration = lifecycle.generation();
        lifecycle.allocateSequence();
        lifecycle.completeSend(initialGeneration, true);

        lifecycle.awaitAuthority();
        assertEquals(1, lifecycle.sequence());
        lifecycle.authorityReceived(false);
        var reboundGeneration = lifecycle.generation();
        assertEquals(2, lifecycle.allocateSequence());
        assertTrue(lifecycle.completeSend(reboundGeneration, true));
    }

    @Test
    void authorityChangeInvalidatesAnInFlightCompletion() {
        var lifecycle = new PlayerStateReportLifecycle();
        lifecycle.beginSession();
        lifecycle.authorityReceived(false);
        var staleGeneration = lifecycle.generation();
        lifecycle.allocateSequence();

        lifecycle.authorityReceived(true);

        assertFalse(lifecycle.completeSend(staleGeneration, true));
        assertEquals(PlayerStateReportLifecycle.Phase.NEED_FULL, lifecycle.phase());
    }

    @Test
    void sameAuthorityFullEchoDoesNotStartAReportLoop() {
        var lifecycle = new PlayerStateReportLifecycle();
        lifecycle.beginSession();
        lifecycle.authorityReceived(false);
        var generation = lifecycle.generation();
        lifecycle.allocateSequence();
        lifecycle.completeSend(generation, true);

        lifecycle.authorityReceived(false);

        assertEquals(generation, lifecycle.generation());
        assertEquals(PlayerStateReportLifecycle.Phase.ACTIVE, lifecycle.phase());
    }

    @Test
    void newConnectionResetsTheSequence() {
        var lifecycle = new PlayerStateReportLifecycle();
        lifecycle.beginSession();
        lifecycle.authorityReceived(false);
        lifecycle.allocateSequence();

        lifecycle.beginSession();

        assertEquals(0, lifecycle.sequence());
        assertEquals(PlayerStateReportLifecycle.Phase.WAIT_AUTHORITY, lifecycle.phase());
    }
}
