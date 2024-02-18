package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.renderer.CustomPlayerRenderer;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.NativeRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ReplacePlayerHandRenderEvent {
    @SubscribeEvent
    public static void onRenderHand(RenderArmEvent event) {
        if (GeneralConfig.DISABLE_SELF_MODEL.get()) {
            return;
        }
        if (GeneralConfig.DISABLE_SELF_HANDS.get()) {
            return;
        }
        event.setCanceled(true);
        AbstractClientPlayer player = event.getPlayer();
        player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
            ResourceLocation modelId = cap.getModelId();
            final GeoModel geoModel = GeckoLibCache.getInstance().getGeoModels().get(ModelIdUtil.getArmId(modelId));
            if (geoModel == null || !hasArmBone(event.getArm(), geoModel)) {
                return;
            }
            CustomPlayerRenderer instance = RegisterEntityRenderersEvent.getPlayerRenderer();
            final PoseStack poseStack = event.getPoseStack();
            MultiBufferSource multiBufferSource = event.getMultiBufferSource();

            CustomPlayerEntity customPlayer = cap.getAnimatable();
            customPlayer.setTexture(cap.getTextureLocation());
            if (MinecraftForge.EVENT_BUS.post(new SpecialPlayerRenderEvent(player, customPlayer, modelId))) {
                return;
            }
            RenderType renderType = RenderType.entityTranslucent(customPlayer.getTexture());
            final VertexConsumer buffer = multiBufferSource.getBuffer(renderType);
            final int packedLight = event.getPackedLight();
            if (instance != null) {
                if (event.getArm() == HumanoidArm.LEFT) {
                    poseStack.pushPose();
                    poseStack.translate(0.25, 1.8, 0);
                    poseStack.scale(-1, -1, 1);
                    NativeRenderer.renderModel(buffer, poseStack.last(), geoModel, geoModel.getInitialState(), NativeRenderer.RENDER_MODE_LEFT_ARM, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
                    poseStack.popPose();
                }
                if (event.getArm() == HumanoidArm.RIGHT) {
                    poseStack.pushPose();
                    poseStack.translate(-0.25, 1.8, 0);
                    poseStack.scale(-1, -1, 1);
                    NativeRenderer.renderModel(buffer, poseStack.last(), geoModel, geoModel.getInitialState(), NativeRenderer.RENDER_MODE_RIGHT_ARM, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
                    poseStack.popPose();
                }
            }
        });
    }

    private static boolean hasArmBone(HumanoidArm arm, GeoModel model) {
        if (arm == HumanoidArm.LEFT) {
            return model.hasFirstPersonLeftArm;
        } else {
            return model.hasFirstPersonRightArm;
        }
    }
}
