package com.elfmcys.ysm.model.domain;

import com.elfmcys.ysm.model.catalog.CatalogRootKind;

import java.util.Map;
import java.util.Objects;

public record ModelPackDescriptor(
        CatalogRootKind rootKind,
        String hierarchy,
        String name,
        String description,
        Map<String, LocalizedText> translations,
        byte[] coverHash,
        String coverFormat,
        int coverSize) implements Comparable<ModelPackDescriptor> {

    public ModelPackDescriptor {
        Objects.requireNonNull(rootKind, "rootKind");
        hierarchy = normalizeHierarchy(hierarchy);
        name = Objects.requireNonNullElse(name, "");
        description = Objects.requireNonNullElse(description, "");
        translations = Map.copyOf(translations);
        coverHash = coverHash == null ? new byte[0] : coverHash.clone();
        coverFormat = Objects.requireNonNullElse(coverFormat, "");
        if (coverHash.length != 0 && coverHash.length != ModelHash.SIZE) {
            throw new IllegalArgumentException("Pack cover hash must be empty or 32 bytes");
        }
        if (coverSize < 0) {
            throw new IllegalArgumentException("Pack cover size cannot be negative");
        }
    }

    @Override
    public byte[] coverHash() {
        return coverHash.clone();
    }

    @Override
    public int compareTo(ModelPackDescriptor other) {
        var byOrigin = rootKind.compareTo(other.rootKind);
        return byOrigin != 0 ? byOrigin : hierarchy.compareTo(other.hierarchy);
    }

    private static String normalizeHierarchy(String hierarchy) {
        if (hierarchy == null || hierarchy.isBlank()) {
            return "";
        }
        var path = new ModelPath(hierarchy).value();
        return path + "/";
    }

    public record LocalizedText(String name, String description) {
        public LocalizedText {
            name = Objects.requireNonNullElse(name, "");
            description = Objects.requireNonNullElse(description, "");
        }
    }
}
