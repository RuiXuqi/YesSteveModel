package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IGeoRenderer<T extends AnimatableEntity<?>> {
    MultiBufferSource getCurrentRTB();

    default void setCurrentRTB(MultiBufferSource bufferSource) {
    }

    default void preRender(GeoModelState modelState, T instance, float partialTick, RenderType type, PoseStack poseStack, @Nullable MultiBufferSource bufferSource,
                           @Nullable VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        setCurrentRTB(bufferSource);
        renderEarly(instance, poseStack, partialTick, bufferSource, buffer, packedLight,
                packedOverlay, red, green, blue, alpha);
        renderLate(instance, poseStack, partialTick, bufferSource, buffer, packedLight,
                packedOverlay, red, green, blue, alpha);
    }

    default void render(GeoModelState modelState, T instance, float partialTick, RenderType type, PoseStack poseStack, @Nullable MultiBufferSource bufferSource,
                        int textureIndex, @Nullable VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (buffer == null) {
            buffer = bufferSource.getBuffer(type);
        }
        // 渲染所有骨骼
        NativeRenderer.renderModel(buffer, poseStack.last(), modelState.model(), modelState.inputState(), modelState.outputState(), textureIndex, NativeRenderer.RENDER_MODE_ALL, packedLight, packedOverlay, red, green, blue, alpha);
        // 由于此时我们至少渲染了一次，因此让我们将循环设置为重复
        setCurrentModelRenderCycle(EModelRenderCycle.REPEATED);
    }

    default void renderEarly(T instance, PoseStack poseStack, float partialTick,
                             @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight,
                             int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (getCurrentModelRenderCycle() == EModelRenderCycle.INITIAL) {
            float width = instance.getWidthScale();
            float height = instance.getHeightScale();
            poseStack.scale(width, height, width);
        }
    }

    default void renderLate(T instance, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource,
                            @Nullable VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue,
                            float alpha) {
    }

    default RenderType getRenderType(ResourceLocation texture) {
        return CustomTranslucentRenderType.create(texture);
    }

    default Color getRenderColor(T instance, float partialTick, PoseStack poseStack,
                                 @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight) {
        return Color.WHITE;
    }

    @NotNull
    default IRenderCycle getCurrentModelRenderCycle() {
        return EModelRenderCycle.INITIAL;
    }

    default void setCurrentModelRenderCycle(IRenderCycle cycle) {
    }

    default boolean isAsyncScope() {
        return NativeRenderer.isAsyncScope();
    }
}
