package com.elfmcys.ysm.model.source;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable configuration and catalog identity of one model source. */
public record SourceId(String value) implements Comparable<SourceId> {
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");

    public SourceId {
        Objects.requireNonNull(value, "value");
        if (!VALID_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid model source id: " + value);
        }
    }

    @Override
    public int compareTo(SourceId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
