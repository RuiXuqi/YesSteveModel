package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.client.entity.CustomArrowEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import net.minecraft.resources.ResourceLocation;

public class CustomArrowModel extends AnimatedGeoModel<CustomArrowEntity> {
    public static final ResourceLocation DEFAULT_ARROW_MODEL = ModelIdUtil.DEFAULT_ARROW_MODEL_ID;
    public static final ResourceLocation DEFAULT_ARROW_ANIMATION = ModelIdUtil.DEFAULT_ARROW_MODEL_ID;
    public static final ResourceLocation DEFAULT_TEXTURE = ModelIdUtil.DEFAULT_TEXTURE_ID;

    @Override
    public ResourceLocation getModelLocation(CustomArrowEntity arrowEntity) {
        return arrowEntity.getMainModel();
    }

    @Override
    public ResourceLocation getTextureLocation(CustomArrowEntity arrowEntity) {
        return arrowEntity.getTexture();
    }

    @Override
    public ResourceLocation getAnimationFileLocation(CustomArrowEntity arrowEntity) {
        return arrowEntity.getAnimation();
    }
}