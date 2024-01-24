package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.client.animation.AnimationRegister;
import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.compat.IrisCompat;
import com.elfmcys.yesstevemodel.client.gui.DebugAnimationScreen;
import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerScreen;
import com.elfmcys.yesstevemodel.client.input.*;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.DEBUG_TEXT;

public class ClientSetupEvent {
    public static void onClientSetup(FMLClientSetupEvent event) {
        AnimationRegister.registerAnimationState();
        FirstPersonCompat.init();
        IrisCompat.init();
        initCoreClient();
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PlayerModelScreenKey.PLAYER_MODEL_KEY);
        event.register(AnimationRouletteKey.ANIMATION_ROULETTE_KEY);
        event.register(DebugAnimationKey.DEBUG_ANIMATION_KEY);
        event.register(ExtraPlayerConfigKey.EXTRA_PLAYER_RENDER_KEY);
        ExtraAnimationKey.registerKeyBinding(event);
    }

    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(DEBUG_TEXT.id(), "ysm_debug_info", new DebugAnimationScreen());
        event.registerAbove(DEBUG_TEXT.id(), "ysm_extra_player", new ExtraPlayerScreen());
    }

    private static void initCoreClient() {
        Component error = (Component) nInitCoreClient();
        if(error != null) {
            throw new ReportedException(new CrashReport("YSM Client Initialization Failed,",
                    new RuntimeException(error.getString())));
        }
    }

    public static native Object nInitCoreClient();
}
