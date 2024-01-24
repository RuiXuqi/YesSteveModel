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
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;

public class RenderFirstPlayerBackground {
    /**
     * 因为 RenderHandEvent 可有几率会渲染多次，所以为了避免多次渲染，这样设计
     */
    private static boolean ALREADY_RENDERED = false;

    public static void onRenderLevelLase(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            ALREADY_RENDERED = false;
        }
    }

    public static void onRenderHand(RenderHandEvent event) {
        if (GeneralConfig.DISABLE_SELF_MODEL.get()) {
            return;
        }
        if (GeneralConfig.DISABLE_SELF_HANDS.get()) {
            return;
        }
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null || ALREADY_RENDERED) {
            return;
        }
        ALREADY_RENDERED = true;
        player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
            ResourceLocation modelId = cap.getModelId();
            final GeoModel geoModel = GeckoLibCache.getInstance().getGeoModels().get(ModelIdUtil.getArmId(modelId));
            if (geoModel == null || !geoModel.hasfirstPersonBackground) {
                return;
            }
            CustomPlayerRenderer instance = RegisterEntityRenderersEvent.getPlayerRenderer();
            final PoseStack poseStack = event.getPoseStack();
            MultiBufferSource multiBufferSource = event.getMultiBufferSource();
            CustomPlayerEntity customPlayer = cap.getAnimatable();
            if (MinecraftForge.EVENT_BUS.post(new SpecialPlayerRenderEvent(player, customPlayer, modelId))) {
                return;
            }
            RenderType renderType = RenderType.entityTranslucent(customPlayer.getTexture());
            final VertexConsumer buffer = multiBufferSource.getBuffer(renderType);
            final int packedLight = event.getPackedLight();
            if (instance != null) {
                poseStack.pushPose();
                if (Minecraft.getInstance().options.bobView().get()) {
                    bobView(poseStack, event.getPartialTick(), player);
                }
                poseStack.translate(0, -1.5, 0);
                NativeRenderer.renderModel(buffer, poseStack.last(), geoModel, geoModel.getInitialState(), NativeRenderer.RENDER_MODE_BACKGROUND, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
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
