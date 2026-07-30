package com.elfmcys.ysm.geckolib3.geo;

import com.elfmcys.ysm.geckolib3.core.util.Color;
import com.elfmcys.ysm.geckolib3.model.AnimatableEntity;
import com.elfmcys.ysm.geckolib3.util.EModelRenderCycle;
import com.elfmcys.ysm.geckolib3.util.IRenderCycle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public abstract class GeoEntityRenderer<TEntity extends Entity, T extends AnimatableEntity<TEntity>> extends EntityRenderer<TEntity> implements IGeoRenderer<T> {
    private IRenderCycle currentModelRenderCycle = EModelRenderCycle.INITIAL;

    public GeoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public void render(T animatable, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        var mc = Minecraft.getInstance();
        var data = animatable.update(partialTick);
        if (data != null && mc.player != null) {
            var entity = animatable.getEntity();
            var bodyVisible = !entity.isInvisibleTo(mc.player);
            var glowing = mc.shouldEntityAppearGlowing(entity);
            var renderType = getRenderType(data.texture, bodyVisible, glowing, false);

            if (renderType != null && (bodyVisible || glowing)) {
                setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
                poseStack.pushPose();
                try {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
                    renderEarly(data, animatable, poseStack);
                    render(data, animatable, renderType, poseStack, bufferSource, null,
                            packedLight, getPackedOverlay(entity, 0), Color.WHITE);
                } finally {
                    poseStack.popPose();
                }
            }
        }
        super.render(animatable.getEntity(), yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void renderEarly(GeoRenderData data, T animatable, PoseStack poseStack) {
        IGeoRenderer.super.renderEarly(data, animatable, poseStack);
    }

    public static int getPackedOverlay(Entity entity, float uIn) {
        return OverlayTexture.pack(OverlayTexture.u(uIn), OverlayTexture.v(false));
    }

    @Override
    @NotNull
    public IRenderCycle getCurrentModelRenderCycle() {
        return this.currentModelRenderCycle;
    }

    @Override
    public void setCurrentModelRenderCycle(IRenderCycle currentModelRenderCycle) {
        this.currentModelRenderCycle = currentModelRenderCycle;
    }
}
