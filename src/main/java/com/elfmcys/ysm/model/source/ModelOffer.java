package com.elfmcys.ysm.model.source;

import com.elfmcys.ysm.model.domain.ModelPath;
import com.elfmcys.ysm.model.domain.ModelDescriptor;

import java.util.Objects;

/** One source-specific way to resolve a model identity. */
public record ModelOffer(SourceId sourceId,
                         String namespace,
                         ModelPath path,
                         AccessPolicy accessPolicy,
                         ModelDescriptor descriptor) {
    public ModelOffer {
        Objects.requireNonNull(sourceId, "sourceId");
        namespace = Objects.requireNonNull(namespace, "namespace").trim();
        if (namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace must not be empty");
        }
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(accessPolicy, "accessPolicy");
        Objects.requireNonNull(descriptor, "descriptor");
    }

    public ModelAssetSubject.Model subject() {
        return new ModelAssetSubject.Model(descriptor.modelHash(), descriptor.descriptorHash());
    }
}
