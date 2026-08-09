package com.elfmcys.ysm.model.source;

import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.ModelPackDescriptor;

import java.util.Map;

import java.util.Objects;

/** Minimal source-independent catalog entry for a model pack. */
public record PackOffer(SourceId sourceId, ModelAssetSubject.Pack subject,
                        String name, String description,
                        Map<String, ModelPackDescriptor.LocalizedText> translations,
                        Hash256 coverHash, String coverFormat, int coverSize,
                        AccessPolicy accessPolicy) {
    public PackOffer {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(subject, "subject");
        name = Objects.requireNonNullElse(name, "");
        description = Objects.requireNonNullElse(description, "");
        translations = Map.copyOf(translations);
        coverFormat = Objects.requireNonNullElse(coverFormat, "");
        Objects.requireNonNull(accessPolicy, "accessPolicy");
        if (coverSize < 0) {
            throw new IllegalArgumentException("coverSize must not be negative");
        }
    }
}
