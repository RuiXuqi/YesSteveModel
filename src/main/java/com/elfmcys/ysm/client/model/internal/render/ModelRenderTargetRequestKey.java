package com.elfmcys.ysm.client.model.internal.render;

import com.elfmcys.ysm.client.model.catalog.ModelContentVersion;
import com.elfmcys.ysm.model.domain.Hash256;

import java.util.Objects;

public record ModelRenderTargetRequestKey(Hash256 modelHash, ModelContentVersion contentVersion,
                                          String renderTargetId, String textureName) {
    public ModelRenderTargetRequestKey {
        Objects.requireNonNull(modelHash, "modelHash");
        Objects.requireNonNull(contentVersion, "contentVersion");
        renderTargetId = Objects.requireNonNull(renderTargetId, "renderTargetId");
        if (renderTargetId.isBlank()) {
            throw new IllegalArgumentException("Render target id cannot be blank");
        }
        textureName = Objects.requireNonNullElse(textureName, "");
    }
}
