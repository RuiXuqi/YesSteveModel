package com.elfmcys.ysm.client.model.catalog;

/** Process-local incarnation of one model's selected immutable backing. */
public record ModelContentVersion(long value) implements Comparable<ModelContentVersion> {
    public ModelContentVersion {
        if (value <= 0) {
            throw new IllegalArgumentException("Model content version must be positive");
        }
    }

    @Override
    public int compareTo(ModelContentVersion other) {
        return Long.compare(value, other.value);
    }
}
