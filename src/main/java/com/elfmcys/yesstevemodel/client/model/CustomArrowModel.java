package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.info.type.ProjectileType;
import com.elfmcys.yesstevemodel.client.entity.CustomArrowEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatedGeoModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CustomArrowModel extends AnimatedGeoModel<CustomArrowEntity> {
    @Override
    public GeoModel getModel(String location) {
        return ClientModelManager.getModel(location).map(model -> model.projectileModels().get(ProjectileType.ARROW).model()).orElse(null);
    }

    @Override
    public String getModelLocation(CustomArrowEntity arrowEntity) {
        return arrowEntity.getModelId();
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(CustomArrowEntity arrowEntity) {
        return ClientModelManager.getModel(arrowEntity.getModelId()).map(model -> model.projectileModels().get(ProjectileType.ARROW).texture()).orElse(MissingTextureAtlasSprite.getLocation());
    }

    @Override
    public Animation getAnimation(String name, CustomArrowEntity animatable) {
        return ClientModelManager.getProjectileModel(animatable.getModelId(), ProjectileType.ARROW).map(model -> model.animations().get(name))
                .orElse(null);
    }
}