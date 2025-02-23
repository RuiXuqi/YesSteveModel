package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.client.animation.AnimationRegister;
import com.elfmcys.yesstevemodel.client.compat.*;
import com.elfmcys.yesstevemodel.client.compat.backpack.sophisticated.SophisticatedCompat;
import com.elfmcys.yesstevemodel.client.compat.bettercombat.BetterCombatCompat;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;
import com.elfmcys.yesstevemodel.client.compat.curios.CuriosCompat;
import com.elfmcys.yesstevemodel.client.compat.parcool.ParCoolCompat;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeCompat;
import com.elfmcys.yesstevemodel.client.compat.swem.SwemCompat;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TlmCompat;
import com.elfmcys.yesstevemodel.client.gui.DebugAnimationScreen;
import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerScreen;
import com.elfmcys.yesstevemodel.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.DEBUG_TEXT;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetupEvent {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        AnimationRegister.registerAnimationState();
        CuriosCompat.init();
        FirstPersonCompat.init();
        BetterCombatCompat.init();
        IrisCompat.init();
        OptifineCompat.init();
        CosmeticArmorCompat.init();
        ElytraSlotCompat.init();
        TACZCompat.init();
        TlmCompat.init();
        CarryOnCompat.init();
        ParCoolCompat.init();
        SlashBladeCompat.init();
        SwemCompat.init();
        SophisticatedCompat.init();
        initCoreClient();
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PlayerModelScreenKey.PLAYER_MODEL_KEY);
        event.register(AnimationRouletteKey.ANIMATION_ROULETTE_KEY);
        event.register(AnimationRouletteKey.LOCK_ROULETTE_KEY);
        event.register(DebugAnimationKey.DEBUG_ANIMATION_KEY);
        event.register(ExtraPlayerConfigKey.EXTRA_PLAYER_RENDER_KEY);
        ExtraAnimationKey.registerKeyBinding(event);
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(DEBUG_TEXT.id(), "ysm_debug_info", new DebugAnimationScreen());
        event.registerAbove(DEBUG_TEXT.id(), "ysm_extra_player", new ExtraPlayerScreen());
    }

    private static void initCoreClient() {
        Component error = (Component) nInitCoreClient();
        if (error != null) {
            throw new RuntimeException("YSM Client Initialization Failed: " + error.getString(256));
        }
    }

    public static native Object nInitCoreClient();
}
