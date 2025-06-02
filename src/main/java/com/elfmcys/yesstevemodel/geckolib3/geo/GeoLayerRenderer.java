package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public abstract class GeoLayerRenderer<T extends AnimatableEntity<?>> {
    public GeoLayerRenderer() {
    }

    public RenderType getRenderType(ResourceLocation textureLocation) {
        return RenderType.entityCutout(textureLocation);
    }

    public abstract void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn,
                                T animatableEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                float netHeadYaw, float headPitch);
}