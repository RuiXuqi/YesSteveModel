package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class GeoProjectilesRenderer<T extends GeoInstance<?, ?>> extends EntityRenderer<AbstractArrow> implements IGeoRenderer<T> {
    protected Matrix4f dispatchedMat = new Matrix4f();
    protected Matrix4f renderEarlyMat = new Matrix4f();
    private IRenderCycle currentModelRenderCycle = EModelRenderCycle.INITIAL;
    protected MultiBufferSource rtb = null;

    public GeoProjectilesRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Nullable
    protected abstract T getGeoInstance(AbstractArrow entity);

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void render(AbstractArrow entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (Minecraft.getInstance().player != null && !entity.isInvisibleTo(Minecraft.getInstance().player)) {
            T instance = getGeoInstance(entity);
            AnimationEvent<?> event = NativeRenderer.isAsyncScope() ? instance.waitOrUpdate(partialTick) : instance.syncUpdate(partialTick);
            if(event != null) {
                this.dispatchedMat = new Matrix4f(poseStack.last().pose());
                setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
                Color renderColor = getRenderColor(instance, partialTick, poseStack, bufferSource, null, packedLight);
                RenderType renderType = getRenderType(instance, partialTick, poseStack, bufferSource, null, packedLight,
                        instance.isModelPresent() ? instance.getTextureLocation() : ModelIdUtil.DEFAULT_ARROW_TEXTURE_ID);
                GeoModelState model = instance.getAnimatableModel().getCurrentModel();
                render(model, instance, partialTick, renderType, poseStack, bufferSource, null, packedLight, getPackedOverlay(entity, 0), renderColor.getRed() / 255f, renderColor.getGreen() / 255f, renderColor.getBlue() / 255f, renderColor.getAlpha() / 255f);
                poseStack.popPose();
            }
        }
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.renderEarlyMat = new Matrix4f(poseStack.last().pose());
        IGeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public static int getPackedOverlay(AbstractArrow entity, float uIn) {
        return OverlayTexture.pack(OverlayTexture.u(uIn), OverlayTexture.v(false));
    }

    @Override
    @Nonnull
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

    @Override
    @Deprecated
    public ResourceLocation getTextureLocation(AbstractArrow entity) {
        throw new RuntimeException();
    }
}
