package com.elfmcys.ysm.network.protocol;

/** Aggregate encoded-plus-decoded reservation for one peer's active transfers. */
public final class AssetTransferBudget {
    private final int maxTransfers;
    private final long maxReservedBytes;
    private int activeTransfers;
    private long reservedBytes;

    public AssetTransferBudget(int maxTransfers, long maxReservedBytes) {
        if (maxTransfers <= 0 || maxReservedBytes <= 0) {
            throw new IllegalArgumentException("Transfer budget limits must be positive");
        }
        this.maxTransfers = maxTransfers;
        this.maxReservedBytes = maxReservedBytes;
    }

    public synchronized Reservation reserve(long encodedBytes, long decodedBytes) {
        if (encodedBytes < 0 || decodedBytes < 0
                || encodedBytes > Long.MAX_VALUE - decodedBytes) {
            throw new IllegalArgumentException("Invalid asset transfer reservation");
        }
        var requested = encodedBytes + decodedBytes;
        if (activeTransfers >= maxTransfers || requested > maxReservedBytes - reservedBytes) {
            throw new IllegalStateException("Asset transfer budget exhausted");
        }
        activeTransfers++;
        reservedBytes += requested;
        return new Reservation(requested);
    }

    public synchronized int activeTransfers() {
        return activeTransfers;
    }

    public synchronized long reservedBytes() {
        return reservedBytes;
    }

    public final class Reservation implements AutoCloseable {
        private final long bytes;
        private boolean closed;

        private Reservation(long bytes) {
            this.bytes = bytes;
        }

        @Override
        public void close() {
            synchronized (AssetTransferBudget.this) {
                if (closed) {
                    return;
                }
                closed = true;
                activeTransfers--;
                reservedBytes -= bytes;
            }
        }
    }
}
