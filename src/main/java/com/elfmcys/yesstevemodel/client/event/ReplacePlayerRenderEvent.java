package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderPlayerEvent;

public class ReplacePlayerRenderEvent {
    public static void onRender(RenderPlayerEvent.Pre event) {
        Player playerRender = event.getEntity();
        LocalPlayer playerSelf = Minecraft.getInstance().player;
        if (playerRender.equals(playerSelf) && GeneralConfig.DISABLE_SELF_MODEL.get()) {
            return;
        }
        if (!playerRender.equals(playerSelf) && GeneralConfig.DISABLE_OTHER_MODEL.get()) {
            return;
        }
        if(!playerRender.getCapability(PlayerGeoCapabilityProvider.CAP).map(GeoInstance::isInitialized).orElse(false)) {
            return;
        }
        event.setCanceled(true);
        RegisterEntityRenderersEvent.getPlayerRenderer().render((AbstractClientPlayer) event.getEntity(), event.getEntity().getYRot(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }
}
