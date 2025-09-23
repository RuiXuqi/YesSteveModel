package com.elfmcys.yesstevemodel.client.texture;

import com.elfmcys.yesstevemodel.info.type.PBRTextureType;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.Map;

public interface PBRTextureSet {
    Map<PBRTextureType, ? extends AbstractTexture> getPBRTextures();
}
