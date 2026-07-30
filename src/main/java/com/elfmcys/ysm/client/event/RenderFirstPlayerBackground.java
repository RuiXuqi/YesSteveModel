package com.elfmcys.ysm.client.event;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.ysm.client.entity.CustomPlayerEntity;
import com.elfmcys.ysm.client.model.ClientModel;
import com.elfmcys.ysm.client.renderer.CustomPlayerRenderer;
import com.elfmcys.ysm.config.ClientConfig;
import com.elfmcys.ysm.event.api.SpecialPlayerRenderEvent;
import com.elfmcys.ysm.geckolib3.geo.CustomTranslucentRenderType;
import com.elfmcys.ysm.geckolib3.geo.NativeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RenderFirstPlayerBackground {
    /**
     * 因为 RenderHandEvent 可有几率会渲染多次，所以为了避免多次渲染，这样设计
     */
    private static boolean ALREADY_RENDERED = false;

    @SubscribeEvent
    public static void onRenderLevelLase(RenderLevelStageEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            ALREADY_RENDERED = false;
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        if (ClientConfig.DISABLE_SELF_MODEL.get()) {
            return;
        }
        if (ClientConfig.DISABLE_SELF_HANDS.get()) {
            return;
        }
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null || ALREADY_RENDERED) {
            return;
        }
        ALREADY_RENDERED = true;
        player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
            if (!cap.isInitializedAndEnabled()) {
                return;
            }
            String modelId = cap.getModelId();
            ClientModel model = cap.getModelContainer();
            if (model == null || !model.playerModel().armModel().hasFirstPersonBackground) {
                return;
            }
            CustomPlayerRenderer renderer = RegisterEntityRenderersEvent.getPlayerRenderer();
            final PoseStack poseStack = event.getPoseStack();
            MultiBufferSource multiBufferSource = event.getMultiBufferSource();
            CustomPlayerEntity customPlayer = cap;
            if (MinecraftForge.EVENT_BUS.post(new SpecialPlayerRenderEvent(player, customPlayer, modelId))) {
                return;
            }

            ResourceLocation textureLocation = cap.getTextureLocation();
            int textureIndex = cap.getTextureIndex();
            var vertexConsumer = multiBufferSource.getBuffer(CustomTranslucentRenderType.create(textureLocation));

            if (renderer != null) {
                poseStack.pushPose();
                if (Minecraft.getInstance().options.bobView().get()) {
                    bobView(poseStack, event.getPartialTick(), player);
                }
                poseStack.translate(0, -1.5, 0);
                NativeRenderer.renderModel(vertexConsumer, poseStack.last(), model.playerModel().armModel(), model.playerModel().armModel().getInitialState(), null, textureIndex, NativeRenderer.RENDER_MODE_BACKGROUND, event.getPackedLight(), OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
                poseStack.popPose();
            }
        });
    }

    private static void bobView(PoseStack pMatrixStack, float pPartialTicks, Player player) {
        float walk = player.walkDist - player.walkDistO;
        float walk2 = -(player.walkDist + walk * pPartialTicks);
        float lerp = Mth.lerp(pPartialTicks, player.oBob, player.bob);
        pMatrixStack.translate(-Mth.sin(walk2 * (float) Math.PI) * lerp * 0.5F, Math.abs(Mth.cos(walk2 * (float) Math.PI) * lerp), 0.0D);
        pMatrixStack.mulPose(Axis.ZN.rotationDegrees(Mth.sin(walk2 * (float) Math.PI) * lerp * 3.0F));
        pMatrixStack.mulPose(Axis.XN.rotationDegrees(Math.abs(Mth.cos(walk2 * (float) Math.PI - 0.2F) * lerp) * 5.0F));
    }
}
