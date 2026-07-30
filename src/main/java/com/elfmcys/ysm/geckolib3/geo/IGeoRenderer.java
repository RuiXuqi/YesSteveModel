package com.elfmcys.ysm.geckolib3.geo;

import com.elfmcys.ysm.geckolib3.core.util.Color;
import com.elfmcys.ysm.geckolib3.model.AnimatableEntity;
import com.elfmcys.ysm.geckolib3.util.EModelRenderCycle;
import com.elfmcys.ysm.geckolib3.util.IRenderCycle;
import com.elfmcys.ysm.natives.render.NativeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public interface IGeoRenderer<T extends AnimatableEntity<?>> {
    default void preRender(GeoRenderData data, T animatable, float partialTick, PoseStack poseStack,
                           @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                           int packedLight, int packedOverlay, Color color) {
        renderEarly(data, animatable, poseStack);
        renderLate(data, animatable, partialTick, poseStack, bufferSource, buffer,
                packedLight, packedOverlay, color);
    }

    default void render(GeoRenderData data, T animatable,
                        RenderType type, PoseStack poseStack, @Nullable MultiBufferSource bufferSource,
                        @Nullable VertexConsumer buffer, int packedLight, int packedOverlay, Color color) {
        if (buffer == null) {
            buffer = Objects.requireNonNull(bufferSource, "bufferSource").getBuffer(type);
        }
        var modelState = data.modelState;
        NativeRenderer.render(
                buffer, poseStack.last(), modelState.getNativeState(), modelState.getVertexCount(),
                packedLight, packedOverlay, color.getColor(), data.ctx.nativeType());
        animatable.countRender();
        setCurrentModelRenderCycle(EModelRenderCycle.REPEATED);
    }

    default void renderEarly(GeoRenderData data, T animatable, PoseStack poseStack) {
        if (getCurrentModelRenderCycle() == EModelRenderCycle.INITIAL) {
            poseStack.scale(data.widthScale, data.heightScale, data.widthScale);
        }
    }

    default void renderLate(GeoRenderData data, T animatable, float partialTick, PoseStack poseStack,
                            @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                            int packedLight, int packedOverlay, Color color) {
    }

    @Nullable
    default RenderType getRenderType(ResourceLocation texture, boolean visible, boolean glowing, boolean translucent) {
        if (visible) {
            return translucent ? CustomTranslucentRenderType.create(texture) : RenderType.entityCutoutNoCull(texture);
        }
        return glowing ? RenderType.outline(texture) : null;
    }

    @NotNull
    default IRenderCycle getCurrentModelRenderCycle() {
        return EModelRenderCycle.INITIAL;
    }

    default void setCurrentModelRenderCycle(IRenderCycle cycle) {
    }
}
