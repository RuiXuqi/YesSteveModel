package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.minecraft.resources.ResourceLocation;

public final class ModelIdUtil {
    public static final String LEGACY_PREFIX = "yes_steve_model:";
    public static final String DEFAULT_MODEL_ID = "default";
    public static final String DEFAULT_TEXTURE_NAME = "default";
    public static final ResourceLocation DEFAULT_TEXTURE_ID = new ResourceLocation(YesSteveModel.MOD_ID, "default/0");
    public static final String ARROW_TEXTURE_NAME_PLACEHOLDER = "/ARROW\\";

    public static ResourceLocation getArrowTextureId(String hash) {
        return new ResourceLocation(YesSteveModel.MOD_ID, hash + "/arrow");
    }

    public static String stripLegacyPrefix(String id) {
        if (id.length() > LEGACY_PREFIX.length()) {
            if (id.startsWith(LEGACY_PREFIX)) {
                return id.substring(LEGACY_PREFIX.length());
            }
        }
        return id;
    }
}
