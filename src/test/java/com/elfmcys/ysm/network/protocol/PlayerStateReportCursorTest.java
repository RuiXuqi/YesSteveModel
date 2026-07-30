package com.elfmcys.ysm.network.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStateReportCursorTest {
    @Test
    void firstAcceptedReportMustBeFull() {
        var cursor = new PlayerStateReportCursor();

        assertFalse(cursor.canAccept(1, false));
        assertTrue(cursor.canAccept(2, true));
        cursor.commit(2, true);
        assertTrue(cursor.canAccept(3, false));
    }

    @Test
    void semanticRejectionDoesNotAdvanceTheCursor() {
        var cursor = new PlayerStateReportCursor();

        assertTrue(cursor.canAccept(1, true));
        assertFalse(cursor.initialized());
        assertFalse(cursor.canAccept(2, false));
        assertTrue(cursor.canAccept(3, true));
    }

    @Test
    void comparesUint64SequencesWithoutSignedOverflow() {
        var cursor = new PlayerStateReportCursor();

        cursor.commit(Long.MAX_VALUE, true);
        assertTrue(cursor.canAccept(Long.MIN_VALUE, false));
        cursor.commit(Long.MIN_VALUE, false);
        assertTrue(cursor.canAccept(-1L, false));
        cursor.commit(-1L, false);
        assertFalse(cursor.canAccept(1, true));
    }

    @Test
    void commitRejectsZeroAndNonIncreasingValues() {
        var cursor = new PlayerStateReportCursor();

        assertThrows(IllegalArgumentException.class, () -> cursor.commit(0, true));
        assertThrows(IllegalArgumentException.class, () -> cursor.commit(4, false));
        cursor.commit(4, true);
        assertThrows(IllegalArgumentException.class, () -> cursor.commit(4, true));
        assertThrows(IllegalArgumentException.class, () -> cursor.commit(3, true));
    }
}
