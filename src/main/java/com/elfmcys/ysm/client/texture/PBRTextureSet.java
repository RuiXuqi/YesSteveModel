package com.elfmcys.ysm.client.texture;

import com.elfmcys.ysm.info.type.PBRTextureType;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.Map;

public interface PBRTextureSet {
    Map<PBRTextureType, ? extends AbstractTexture> getPBRTextures();
}
