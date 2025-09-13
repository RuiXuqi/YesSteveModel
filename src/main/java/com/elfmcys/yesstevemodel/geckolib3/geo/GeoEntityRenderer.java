package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public abstract class GeoEntityRenderer<TEntity extends Entity, T extends AnimatableEntity<TEntity>> extends EntityRenderer<TEntity> implements IGeoRenderer<T> {
    protected Matrix4f dispatchedMat = new Matrix4f();
    protected Matrix4f renderEarlyMat = new Matrix4f();
    private IRenderCycle currentModelRenderCycle = EModelRenderCycle.INITIAL;
    protected MultiBufferSource rtb = null;

    public GeoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public void render(T instance, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        AnimationEvent<?> event = isAsyncScope() ? instance.waitOrUpdate(partialTick) : instance.syncUpdate(partialTick);
        if (event != null) {
            var entity = instance.getEntity();
            this.dispatchedMat = new Matrix4f(poseStack.last().pose());
            setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
            Color renderColor = getRenderColor(instance, partialTick, poseStack, bufferSource, null, packedLight);
            RenderType renderType = getRenderType(instance.getTextureLocation());
            GeoModelState model = instance.getLoadedGeoModel();
            render(model, instance, partialTick, renderType, poseStack, bufferSource, 0, null, packedLight, getPackedOverlay(entity, 0), renderColor.getRed() / 255f, renderColor.getGreen() / 255f, renderColor.getBlue() / 255f, renderColor.getAlpha() / 255f);
            poseStack.popPose();
        }
        super.render(instance.getEntity(), yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.renderEarlyMat = new Matrix4f(poseStack.last().pose());
        IGeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
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

    @Override
    public void setCurrentRTB(MultiBufferSource bufferSource) {
        this.rtb = bufferSource;
    }

    @Override
    public MultiBufferSource getCurrentRTB() {
        return this.rtb;
    }
}
