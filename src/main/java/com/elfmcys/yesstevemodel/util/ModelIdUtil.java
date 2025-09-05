package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.YesSteveModel;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;

public final class ModelIdUtil {
    public static final String LEGACY_PREFIX = "yes_steve_model:";
    public static final String DEFAULT_MODEL_ID = "default";
    public static final String DEFAULT_TEXTURE_NAME = "default";
    public static final ResourceLocation DEFAULT_TEXTURE_LOCATION = new ResourceLocation(YesSteveModel.MOD_ID, "default/0");
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

    public static int getModelHashShort(String modelHash) {
        return Integer.parseUnsignedInt(modelHash.substring(0, 8), 16);
    }

    /**
     * "dir1/dir2/id" -> {"id", "dir1/dir2/"}
     * <p/>
     * "id" -> {"id", ""}
     */
    public static Pair<String, String> splitModelPath(String modelPath) {
        var lastSlash = modelPath.lastIndexOf('/');
        if (lastSlash == -1) {
            return Pair.of(modelPath, "");
        } else {
            return Pair.of(modelPath.substring(lastSlash + 1), modelPath.substring(0, lastSlash + 1));
        }
    }
}
