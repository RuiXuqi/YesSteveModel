package com.elfmcys.ysm.client.model.data;

import com.elfmcys.ysm.info.ModelInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record ModelRenderTargetBuildInput(String renderTargetId, RenderTargetData target,
                               CommonAssetData assets, @NotNull ModelInfo info) {
    public ModelRenderTargetBuildInput {
        if (Objects.requireNonNull(renderTargetId, "renderTargetId").isBlank()) {
            throw new IllegalArgumentException("Render target id cannot be blank");
        }
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(assets, "assets");
        Objects.requireNonNull(info, "info");
    }
}
