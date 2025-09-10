package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.ProjectileAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.entity.CustomProjectileEntity;
import com.elfmcys.yesstevemodel.client.event.RegisterEntityRenderersEvent;
import com.elfmcys.yesstevemodel.config.ClientConfig;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoProjectilesRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class CustomProjectileRenderer extends GeoProjectilesRenderer<Projectile, CustomProjectileEntity> {
    public CustomProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    /**
     * 将所有 mixin 里的渲染方法集中到此处执行
     */
    public static void renderInMixin(Projectile entity, float yaw, float partialTick, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight, CallbackInfo callback) {
        renderInMixin(entity, yaw, partialTick, poseStack, bufferSource, packedLight, () -> {
        }, callback);
    }

    /**
     * 将所有 mixin 里的渲染方法集中到此处执行
     */
    public static void renderInMixin(Projectile entity, float yaw, float partialTick, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight, Runnable runnable,
                                     CallbackInfo callback) {
        if (!YesSteveModel.isAvailable() || ClientConfig.DISABLE_PROJECTILE_MODEL.get()) {
            return;
        }
        entity.getCapability(ProjectileAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
            if (cap.isInitialized() && cap.isModelPresent()) {
                RegisterEntityRenderersEvent.getProjectRenderer().render(cap, yaw, partialTick, poseStack, bufferSource, packedLight);
                runnable.run();
                callback.cancel();
            }
        });
    }

    @Override
    public void render(Projectile entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (Minecraft.getInstance().player == null || entity.isInvisibleTo(Minecraft.getInstance().player)) {
            return;
        }
        entity.getCapability(ProjectileAnimatableCapabilityProvider.CAP).ifPresent(cap -> render(cap, yaw, partialTick, poseStack, bufferSource, packedLight));
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(Projectile entity) {
        return entity.getCapability(ProjectileAnimatableCapabilityProvider.CAP).map(CustomProjectileEntity::getTextureLocation).orElse(MissingTextureAtlasSprite.getLocation());
    }
}
