package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.client.compat.IrisCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
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

public interface IGeoRenderer<T extends GeoInstance<?, ?>> {
    MultiBufferSource getCurrentRTB();

    default void setCurrentRTB(MultiBufferSource bufferSource) {
    }

    default void render(GeoModelState modelState, T instance, float partialTick, PoseStack poseStack, @NotNull MultiBufferSource bufferSource, ResourceLocation textureLocation,
                        int textureIndex, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        setCurrentRTB(bufferSource);
        renderEarly(instance, poseStack, partialTick, bufferSource, textureLocation, packedLight,
                packedOverlay, red, green, blue, alpha);
        renderLate(instance, poseStack, partialTick, bufferSource, textureLocation, packedLight,
                packedOverlay, red, green, blue, alpha);
        if (IrisCompat.isInstalled()) {
            IrisCompat.setupState();
        }
        // 渲染所有骨骼
        NativeRenderer.renderModel(bufferSource, getRenderTypeCutout(textureLocation), getRenderTypeTranslucent(textureLocation), poseStack.last(), modelState.model(), modelState.state(), textureIndex, NativeRenderer.RENDER_MODE_ALL, packedLight, packedOverlay, red, green, blue, alpha);
        // 由于此时我们至少渲染了一次，因此让我们将循环设置为重复
        setCurrentModelRenderCycle(EModelRenderCycle.REPEATED);
    }

    default void renderEarly(T instance, PoseStack poseStack, float partialTick,
                             @Nullable MultiBufferSource bufferSource, @NotNull ResourceLocation texture, int packedLight,
                             int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (getCurrentModelRenderCycle() == EModelRenderCycle.INITIAL) {
            float width = instance.getWidthScale();
            float height = instance.getHeightScale();
            poseStack.scale(width, height, width);
        }
    }

    default void renderLate(T instance, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource,
                            ResourceLocation texture, int packedLight, int packedOverlay, float red, float green, float blue,
                            float alpha) {
    }

    default RenderType getRenderTypeCutout(ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }

    default RenderType getRenderTypeTranslucent(ResourceLocation texture) {
        return GeoTranslucentRenderType.create(texture);
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
