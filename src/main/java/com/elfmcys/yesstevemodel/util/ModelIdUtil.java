package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public final class ModelIdUtil {
    public static final ResourceLocation DEFAULT_MODEL_ID = getModelId("default");

    public static final ResourceLocation DEFAULT_MAIN_MODEL_ID = getMainId(DEFAULT_MODEL_ID);
    public static final ResourceLocation DEFAULT_ARROW_MODEL_ID = getArrowId(DEFAULT_MODEL_ID);

    public static final ResourceLocation DEFAULT_TEXTURE_ID = getTextureId(DEFAULT_MODEL_ID, "default.png");
    public static final ResourceLocation DEFAULT_ARROW_TEXTURE_ID = getArrowTextureId(DEFAULT_MODEL_ID);

    public static final String ARROW_MODEL_NAME = "arrow";
    public static final String ARROW_TEXTURE_NAME = "arrow.png";

    public static ResourceLocation getSubModelId(ResourceLocation id, String subName) {
        String newPath = id.getPath() + "/" + subName;
        return new ResourceLocation(id.getNamespace(), newPath);
    }

    public static ResourceLocation getMainId(ResourceLocation id) {
        return getSubModelId(id, "main");
    }

    public static ResourceLocation getArmId(ResourceLocation id) {
        return getSubModelId(id, "arm");
    }

    public static ResourceLocation getModelIdFromMainId(ResourceLocation mainId) {
        String newPath = mainId.getPath().substring(0, mainId.getPath().length() - 5);
        return new ResourceLocation(mainId.getNamespace(), newPath);
    }

    @Nullable
    public static String getSubNameFromId(ResourceLocation mainId) {
        String[] split = mainId.getPath().split("/", 2);
        if (split.length == 2) {
            return split[1];
        }
        return StringUtils.EMPTY;
    }

    public static ResourceLocation getArrowId(ResourceLocation id) {
        return getSubModelId(id, ARROW_MODEL_NAME);
    }

    public static ResourceLocation getModelId(String modelName) {
        return new ResourceLocation(YesSteveModel.MOD_ID, modelName);
    }

    public static ResourceLocation getTextureId(ResourceLocation modelId, String textureName) {
        return getSubModelId(modelId, textureName);
    }

    public static ResourceLocation getArrowTextureId(ResourceLocation modelId) {
        return getTextureId(modelId, ARROW_TEXTURE_NAME);
    }
}
