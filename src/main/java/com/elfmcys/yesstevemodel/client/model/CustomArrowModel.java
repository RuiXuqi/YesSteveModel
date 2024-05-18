package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.entity.CustomArrowEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatedGeoModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public class CustomArrowModel extends AnimatedGeoModel<CustomArrowEntity> {
    @Override
    public GeoModel getModel(String location) {
        return ClientModelManager.getModel(location).map(ClientModel::arrowModel).orElse(null);
    }

    @Override
    public String getModelLocation(CustomArrowEntity arrowEntity) {
        return arrowEntity.getModelId();
    }

    @Override
    public ResourceLocation getTextureLocation(CustomArrowEntity arrowEntity) {
        return ClientModelManager.getModel(arrowEntity.getModelId()).map(ClientModel::arrowTexture).orElse(MissingTextureAtlasSprite.getLocation());
    }

    @Override
    public Animation getAnimation(String name, CustomArrowEntity animatable) {
        return ClientModelManager.getArrowAnimation(animatable.getModelId(), name)
                .orElse(null);
    }
}