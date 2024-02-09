package com.elfmcys.yesstevemodel.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class MixinWrapper {
    public static void tickAllInstance(final float partialTick) {
        MixinInnerWrapper.tickAllInstance(partialTick);
    }

    public static void beginAsyncScope() {
        MixinInnerWrapper.beginAsyncScope();
    }

    public static void endAsyncScope() {
        MixinInnerWrapper.endAsyncScope();
    }

    @Nullable
    public static Object getInstance(AbstractArrow entity, String modelName) {
        return MixinInnerWrapper.getInstance(entity, modelName);
    }

    public static void getPlayerModelName(Entity entity, Consumer<String> modelNameConsumer) {
        MixinInnerWrapper.getPlayerModelName(entity, modelNameConsumer);
    }

    public static boolean renderArrow(AbstractArrow entity, float pEntityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        return MixinInnerWrapper.renderArrow(entity, pEntityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    public static Object transformPersonPlayerOffset(Object offset) {
        return MixinInnerWrapper.transformPersonPlayerOffset(offset);
    }
}
