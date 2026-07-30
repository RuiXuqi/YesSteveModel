package com.elfmcys.ysm.network.protocol;

/** Tracks accepted uint64 player-state report sequences for one transport connection. */
public final class PlayerStateReportCursor {
    private long lastSequence;
    private boolean initialized;

    public boolean canAccept(long sequence, boolean full) {
        if (sequence == 0 || !initialized && !full) {
            return false;
        }
        return !initialized || Long.compareUnsigned(sequence, lastSequence) > 0;
    }

    public void commit(long sequence, boolean full) {
        if (!canAccept(sequence, full)) {
            throw new IllegalArgumentException("Player-state report sequence was not accepted");
        }
        lastSequence = sequence;
        initialized = true;
    }

    public boolean initialized() {
        return initialized;
    }
}
