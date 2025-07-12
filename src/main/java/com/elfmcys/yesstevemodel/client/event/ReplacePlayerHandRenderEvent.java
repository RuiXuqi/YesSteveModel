package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.renderer.CustomPlayerRenderer;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.CustomTranslucentRenderType;
import com.elfmcys.yesstevemodel.geckolib3.geo.NativeRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
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
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        if (GeneralConfig.DISABLE_SELF_MODEL.get()) {
            return;
        }
        if (GeneralConfig.DISABLE_SELF_HANDS.get()) {
            return;
        }
        event.setCanceled(true);
        AbstractClientPlayer player = event.getPlayer();
        player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
            String modelId = cap.getModelId();
            ClientModel model = ClientModelManager.getModel(modelId).orElse(null);
            if (model == null || !hasArmBone(event.getArm(), model.armModel())) {
                return;
            }
            CustomPlayerRenderer renderer = RegisterEntityRenderersEvent.getPlayerRenderer();
            final PoseStack poseStack = event.getPoseStack();
            MultiBufferSource multiBufferSource = event.getMultiBufferSource();

            CustomPlayerEntity customPlayer = cap;
            SpecialPlayerRenderEvent renderEvent = new SpecialPlayerRenderEvent(player, customPlayer, modelId);
            if (MinecraftForge.EVENT_BUS.post(renderEvent)) {
                return;
            }
            ResourceLocation textureLocation = renderEvent.getTextureLocationOverride() != null ? renderEvent.getTextureLocationOverride() : cap.getTextureLocation();
            int textureIndex = renderEvent.getTextureLocationOverride() == null ? cap.getTextureIndex() : 0;
            var vertexConsumer = multiBufferSource.getBuffer(CustomTranslucentRenderType.create(textureLocation));

            if (renderer != null) {
                if (event.getArm() == HumanoidArm.LEFT) {
                    poseStack.pushPose();
                    poseStack.translate(0.25, 1.8, 0);
                    poseStack.scale(-1, -1, 1);
                    NativeRenderer.renderModel(vertexConsumer, poseStack.last(), model.armModel(), model.armModel().getInitialState(), null, textureIndex, NativeRenderer.RENDER_MODE_LEFT_ARM, event.getPackedLight(), OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
                    poseStack.popPose();
                }
                if (event.getArm() == HumanoidArm.RIGHT) {
                    poseStack.pushPose();
                    poseStack.translate(-0.25, 1.8, 0);
                    poseStack.scale(-1, -1, 1);
                    NativeRenderer.renderModel(vertexConsumer, poseStack.last(), model.armModel(), model.armModel().getInitialState(), null, textureIndex, NativeRenderer.RENDER_MODE_RIGHT_ARM, event.getPackedLight(), OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
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
