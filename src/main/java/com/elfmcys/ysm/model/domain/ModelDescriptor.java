package com.elfmcys.ysm.model.domain;

import com.elfmcys.ysm.format.schema.model.ModelFileView;

import java.util.Objects;

public record ModelDescriptor(ModelHash modelHash, ModelHash descriptorHash,
                              byte[] containerPreamble, byte[] schemaManifest,
                              ModelFileView view) {
    /** The constructor takes ownership of both byte arrays; accessors remain defensive. */
    public ModelDescriptor {
        Objects.requireNonNull(modelHash, "modelHash");
        Objects.requireNonNull(descriptorHash, "descriptorHash");
        Objects.requireNonNull(containerPreamble, "containerPreamble");
        Objects.requireNonNull(schemaManifest, "schemaManifest");
        Objects.requireNonNull(view, "view");
    }

    public boolean sameRepresentation(ModelDescriptor other) {
        return modelHash.equals(other.modelHash) && descriptorHash.equals(other.descriptorHash);
    }

    public boolean hasEncodedRepresentation() {
        return containerPreamble.length != 0 || schemaManifest.length != 0;
    }

    public ModelDescriptor withoutEncodedRepresentation() {
        return new ModelDescriptor(modelHash, descriptorHash, new byte[0], new byte[0], view);
    }

    @Override
    public byte[] containerPreamble() {
        return containerPreamble.clone();
    }

    @Override
    public byte[] schemaManifest() {
        return schemaManifest.clone();
    }
}
