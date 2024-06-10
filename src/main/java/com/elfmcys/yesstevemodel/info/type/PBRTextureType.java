package com.elfmcys.yesstevemodel.info.type;

import net.minecraft.resources.ResourceLocation;

// Native Access
public enum PBRTextureType {
    NORMAL("_n"),
    SPECULAR("_s");

    private final String suffix;

    PBRTextureType(String suffix) {
        this.suffix = suffix;
    }

    public ResourceLocation getId(ResourceLocation uvId) {
        return new ResourceLocation(uvId.getNamespace(), uvId.getPath() + suffix);
    }
}
