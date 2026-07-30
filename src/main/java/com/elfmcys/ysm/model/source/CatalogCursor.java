package com.elfmcys.ysm.model.source;

import java.util.Arrays;
import java.util.Objects;

/** Per-source catalog position. Revisions from different sources are never compared. */
public final class CatalogCursor {
    private final SourceId sourceId;
    private final byte[] epoch;
    private final long revision;

    public CatalogCursor(SourceId sourceId, byte[] epoch, long revision) {
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.epoch = Objects.requireNonNull(epoch, "epoch").clone();
        if (epoch.length != 16) {
            throw new IllegalArgumentException("Catalog epoch must be exactly 16 bytes");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("Catalog revision must not be negative");
        }
        this.revision = revision;
    }

    public SourceId sourceId() {
        return sourceId;
    }

    public byte[] epoch() {
        return epoch.clone();
    }

    public long revision() {
        return revision;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CatalogCursor other
                && sourceId.equals(other.sourceId)
                && revision == other.revision
                && Arrays.equals(epoch, other.epoch);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * sourceId.hashCode() + Arrays.hashCode(epoch)) + Long.hashCode(revision);
    }
}
