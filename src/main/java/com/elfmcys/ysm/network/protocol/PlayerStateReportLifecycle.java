package com.elfmcys.ysm.network.protocol;

/** Client-side report ordering for one authoritative player-state session. */
public final class PlayerStateReportLifecycle {
    public enum Phase {
        WAIT_AUTHORITY,
        NEED_FULL,
        ACTIVE
    }

    private Phase phase = Phase.WAIT_AUTHORITY;
    private long sequence;
    private long generation;

    public long beginSession() {
        generation++;
        sequence = 0;
        phase = Phase.WAIT_AUTHORITY;
        return generation;
    }

    public long awaitAuthority() {
        generation++;
        phase = Phase.WAIT_AUTHORITY;
        return generation;
    }

    public void authorityReceived(boolean authorityChanged) {
        if (phase == Phase.WAIT_AUTHORITY || authorityChanged) {
            generation++;
            phase = Phase.NEED_FULL;
        }
    }

    public boolean canSend() {
        return phase != Phase.WAIT_AUTHORITY;
    }

    public boolean needsFull() {
        return phase == Phase.NEED_FULL;
    }

    public long allocateSequence() {
        if (!canSend()) {
            throw new IllegalStateException("Authoritative player state has not been received");
        }
        var next = sequence + 1;
        if (next == 0) {
            throw new IllegalStateException("Player-state report sequence exhausted");
        }
        sequence = next;
        return sequence;
    }

    public boolean completeSend(long expectedGeneration, boolean full) {
        if (generation != expectedGeneration) {
            return false;
        }
        if (full) {
            phase = Phase.ACTIVE;
        }
        return true;
    }

    public boolean isCurrent(long expectedGeneration) {
        return generation == expectedGeneration;
    }

    public long generation() {
        return generation;
    }

    public long sequence() {
        return sequence;
    }

    public Phase phase() {
        return phase;
    }
}
