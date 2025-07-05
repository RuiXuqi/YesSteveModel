package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.capability.*;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TlmCommonCompat;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CommonEvent {
    @SubscribeEvent
    public static void onSetupEvent(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::init);
        event.enqueueWork(TlmCommonCompat::registerEvent);
        initCoreCommon();
    }

    @SubscribeEvent
    public static void registerCapability(RegisterCapabilitiesEvent event) {
        event.register(ModelInfoCapability.class);
        event.register(ProjectileModelInfoCapability.class);
        event.register(AuthModelsCapability.class);
        event.register(StarModelsCapability.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            event.register(PlayerAnimatableCapability.class);
            event.register(ProjectileAnimatableCapability.class);
        }
    }

    private static void initCoreCommon() {
        Component error = (Component) nInitCoreCommon();
        if (error != null) {
            throw new RuntimeException("YSM Initialization Failed: " + error.getString(256));
        }
        Runtime.getRuntime().addShutdownHook(new Thread(CommonEvent::nShutdown));
    }

    private static native Object nInitCoreCommon();

    private static native void nShutdown();
}
