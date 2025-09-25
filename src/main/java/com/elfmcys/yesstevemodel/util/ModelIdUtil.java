package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.YesSteveModel;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;

public final class ModelIdUtil {
    public static final String DEFAULT_MODEL_ID = "default";
    public static final String DEFAULT_TEXTURE_NAME = "default";

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

    public static String getLastFolderName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int idx = trimmed.lastIndexOf('/');
        return idx >= 0 ? trimmed.substring(idx + 1) : trimmed;
    }

    @SuppressWarnings("removal")
    public static ResourceLocation getModelPackIconId(String hierarchy) {
        return new ResourceLocation(YesSteveModel.MOD_ID, "model_pack_icon/" + hierarchy.hashCode());
    }
}
