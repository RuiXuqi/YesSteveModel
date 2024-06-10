package com.elfmcys.yesstevemodel.info.stats;

import com.elfmcys.yesstevemodel.info.type.PBRTextureType;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

import java.util.Map;

// Native Access
public class ModelTextureStats {
    private final TextureStats uv;
    private final Map<PBRTextureType, TextureStats> pbr;

    // Native Access
    public ModelTextureStats(TextureStats uv, Map<PBRTextureType, TextureStats> pbr) {
        this.uv = uv;
        this.pbr = Reference2ReferenceMaps.unmodifiable(new Reference2ReferenceOpenHashMap<>(pbr));
    }

    public TextureStats uv() {
        return uv;
    }

    public Map<PBRTextureType, TextureStats> pbr() {
        return pbr;
    }
}
