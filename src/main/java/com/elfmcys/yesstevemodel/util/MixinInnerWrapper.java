package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.api.IArrowExtraInfo;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.animation.AnimationParallelTicker;
import com.elfmcys.yesstevemodel.client.event.RegisterEntityRenderersEvent;
import com.elfmcys.yesstevemodel.client.instance.CustomArrowInstance;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.geo.NativeRenderer;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Consumer;

// 避免字节码保护导致 Mixin 出现依赖问题
public class MixinInnerWrapper {
    public static void tickAllInstance(final float partialTick) {
        AnimationParallelTicker.tickAll(partialTick);
    }

    public static void beginAsyncScope() {
        NativeRenderer.beginAsyncScope();
    }

    public static void endAsyncScope() {
        NativeRenderer.endAsyncScope();
    }

    @Nullable
    public static Object getInstance(AbstractArrow entity, String modelName) {
        ResourceLocation arrowModelId = ModelIdUtil.getArrowId(ModelIdUtil.getModelId(modelName));
        if(GeckoLibCache.getInstance().getGeoModels().get(arrowModelId) != null) {
            return new CustomArrowInstance(entity, modelName);
        } else {
            return null;
        }
    }

    public static boolean isArrowRendererDisable() {
        return GeneralConfig.DISABLE_ARROWS_MODEL.get();
    }

    public static void getPlayerModelName(Entity entity, Consumer<String> modelNameConsumer) {
        entity.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(cap -> modelNameConsumer.accept(cap.getModelId().getPath()));
    }

    public static boolean renderArrow(AbstractArrow entity, float pEntityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!MixinInnerWrapper.isArrowRendererDisable() && entity instanceof IArrowExtraInfo) {
            CustomArrowInstance instance = (CustomArrowInstance) ((IArrowExtraInfo) entity).getGeoInstance();
            if (instance != null) {
                RegisterEntityRenderersEvent.getArrowRenderer().render(entity, pEntityYaw, partialTick, poseStack, bufferSource, packedLight);
                return true;
            }
        }
        return false;
    }
}
