package com.elfmcys.yesstevemodel.mixin.client.projectile;

import com.elfmcys.yesstevemodel.client.renderer.CustomProjectileRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.FireworkEntityRenderer;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkEntityRenderer.class)
public class FireworkEntityRendererMixin {
    @Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/world/entity/projectile/FireworkRocketEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", cancellable = true)
    public void render(FireworkRocketEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        CustomProjectileRenderer.renderInMixin(entity, entityYaw, partialTicks, poseStack, buffer, packedLight, ci);
    }
}
