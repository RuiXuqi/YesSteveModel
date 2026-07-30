package com.elfmcys.ysm.model.domain;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Objects;

/** User-visible path within a model origin. It is never used as persisted model identity. */
public record ModelPath(String value) implements Comparable<ModelPath> {
    public ModelPath {
        value = normalize(value);
    }

    public static ModelPath relativeTo(Path root, Path file) {
        return new ModelPath(root.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize()).toString());
    }

    public String parentHierarchy() {
        var index = value.lastIndexOf('/');
        return index < 0 ? "" : value.substring(0, index + 1);
    }

    @Override
    public int compareTo(ModelPath other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }

    private static String normalize(String input) {
        Objects.requireNonNull(input, "model path");
        var parts = input.replace('\\', '/').split("/");
        var normalized = new ArrayDeque<String>();
        for (var part : parts) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                throw new IllegalArgumentException("Model path must not escape its model root: " + input);
            }
            if (part.indexOf('\0') >= 0) {
                throw new IllegalArgumentException("Model path contains NUL");
            }
            normalized.add(part);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Model path must not be empty");
        }
        return String.join("/", normalized);
    }
}
