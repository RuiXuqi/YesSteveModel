package com.elfmcys.ysm.network.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirstOccurrenceTrackerTest {
    @Test
    void resetsOnlyTheSelectedConnection() {
        var tracker = new FirstOccurrenceTracker<Key>();
        var first = new Key("first", "invalid-request");
        var second = new Key("second", "invalid-request");

        assertTrue(tracker.first(first));
        assertFalse(tracker.first(first));
        assertTrue(tracker.first(second));

        tracker.removeIf(key -> key.connection().equals("first"));

        assertTrue(tracker.first(first));
        assertFalse(tracker.first(second));
    }

    private record Key(String connection, String category) {
    }
}
