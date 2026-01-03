package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapability;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.compat.RealCameraCompat;
import com.elfmcys.yesstevemodel.client.compat.bettercombat.BetterCombatCompat;
import com.elfmcys.yesstevemodel.client.compat.ironsspellbooks.IronsSpellBooksCompat;
import com.elfmcys.yesstevemodel.config.ClientConfig;
import com.elfmcys.yesstevemodel.util.PersonView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ReplacePlayerRenderEvent {
    @SubscribeEvent
    public static void onRender(RenderPlayerEvent.Pre event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        Player playerRender = event.getEntity();
        LocalPlayer playerSelf = Minecraft.getInstance().player;
        if (playerRender.equals(playerSelf) && ClientConfig.DISABLE_SELF_MODEL.get()) {
            return;
        }
        if (!playerRender.equals(playerSelf) && ClientConfig.DISABLE_OTHER_MODEL.get()) {
            return;
        }
        if (event.getEntity().isSpectator()) {
            return;
        }
        if (playerRender.getCapability(PlayerAnimatableCapabilityProvider.CAP)
                .map(cap -> !shouldSkipRendering(cap))
                .orElse(false)) {
            event.setCanceled(true);
            RegisterEntityRenderersEvent.getPlayerRenderer().render(
                    event.getEntity(), event.getEntity().getYRot(), event.getPartialTick(),
                    event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
        }
    }

    private static boolean shouldSkipRendering(PlayerAnimatableCapability cap) {
        return !cap.isInitializedAndEnabled()
                || (PersonView.isFirstPersonView(cap)
                    && !(FirstPersonCompat.isRenderingPlayer() || RealCameraCompat.isInstalled())
                    && (BetterCombatCompat.isInstalled() || IronsSpellBooksCompat.isInstalled()));
    }
}
