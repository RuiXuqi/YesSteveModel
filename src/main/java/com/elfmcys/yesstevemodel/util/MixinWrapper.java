package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.client.instance.CustomArrowInstance;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;

import javax.annotation.Nullable;

public class MixinWrapper {
    @Nullable
    public static Object getInstance(AbstractArrow entity, String modelName) {
        ResourceLocation arrowModelId = ModelIdUtil.getArrowId(ModelIdUtil.getModelId(modelName));
        if(GeckoLibCache.getInstance().getGeoModels().get(arrowModelId) != null) {
            return new CustomArrowInstance(entity, modelName);
        } else {
            return null;
        }
    }
}
