package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.client.renderer.CustomFirstPersonArmRenderer;
import com.elfmcys.yesstevemodel.config.ClientConfig;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ReplacePlayerHandRenderEvent {
    @SubscribeEvent
    public static void onRenderHand(RenderArmEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        if (ClientConfig.DISABLE_SELF_MODEL.get()) {
            return;
        }
        if (ClientConfig.DISABLE_SELF_HANDS.get()) {
            return;
        }
        if (!(event.getPlayer() instanceof LocalPlayer player)) {
            return;
        }
        event.setCanceled(true);

        player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
            String modelId = cap.getModelId();
            HumanoidArm arm = event.getArm();
            ClientModel model = cap.getModelContainer();
            if (model == null || !hasArmBone(arm, model.playerModel().armModel())) {
                return;
            }
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource multiBufferSource = event.getMultiBufferSource();
            float partialTick = Minecraft.getInstance().getPartialTick();
            CustomFirstPersonArmRenderer armRenderer = RegisterEntityRenderersEvent.getFirstPersonArmRenderer();
            armRenderer.render(player, model, cap, arm, poseStack, multiBufferSource, event.getPackedLight(), partialTick);
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
