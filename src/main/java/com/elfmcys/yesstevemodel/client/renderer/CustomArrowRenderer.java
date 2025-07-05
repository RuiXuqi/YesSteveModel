package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.ProjectileAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.entity.CustomArrowEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoProjectilesRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.jetbrains.annotations.NotNull;

public class CustomArrowRenderer extends GeoProjectilesRenderer<AbstractArrow, CustomArrowEntity> {
    public CustomArrowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(AbstractArrow entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (Minecraft.getInstance().player == null || entity.isInvisibleTo(Minecraft.getInstance().player)) {
            return;
        }
        entity.getCapability(ProjectileAnimatableCapabilityProvider.CAP).ifPresent(cap -> render(cap, yaw, partialTick, poseStack, bufferSource, packedLight));
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(AbstractArrow entity) {
        return entity.getCapability(ProjectileAnimatableCapabilityProvider.CAP).map(CustomArrowEntity::getTextureLocation).orElse(MissingTextureAtlasSprite.getLocation());
    }
}
