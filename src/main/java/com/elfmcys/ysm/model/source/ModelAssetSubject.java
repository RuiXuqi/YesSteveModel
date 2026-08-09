package com.elfmcys.ysm.model.source;

import com.elfmcys.ysm.model.domain.Hash256;

import java.util.Objects;

/** Source-local identity of the catalog object that owns an asset. */
public sealed interface ModelAssetSubject permits ModelAssetSubject.Model, ModelAssetSubject.Pack {
    record Model(Hash256 modelHash, Hash256 descriptorHash) implements ModelAssetSubject {
        public Model {
            Objects.requireNonNull(modelHash, "modelHash");
            Objects.requireNonNull(descriptorHash, "descriptorHash");
        }
    }

    /** Namespace is opaque to the source; GameServer currently uses the catalog model origin. */
    record Pack(String namespace, String hierarchy) implements ModelAssetSubject {
        public Pack {
            namespace = normalize(namespace, "namespace");
            hierarchy = Objects.requireNonNullElse(hierarchy, "").trim();
        }

        private static String normalize(String value, String name) {
            var normalized = Objects.requireNonNull(value, name).trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(name + " must not be empty");
            }
            return normalized;
        }
    }
}
