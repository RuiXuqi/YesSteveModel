package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.capability.PlayerGeoCapability;
import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

public final class CommonEvent {
    public static void onSetupEvent(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::init);
        initCoreCommon();
    }

    public static void registerCapability(RegisterCapabilitiesEvent event) {
        event.register(ModelInfoCapability.class);
        event.register(AuthModelsCapability.class);
        event.register(StarModelsCapability.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            event.register(PlayerGeoCapability.class);
        }
        // PlayerGeoCapability 不需要持久化
    }

    private static void initCoreCommon() {
        Component error = (Component) nInitCoreCommon();
        if (error != null) {
            throw new ReportedException(new CrashReport("YSM Initialization Failed,",
                    new RuntimeException(error.getString())));
        }
        Runtime.getRuntime().addShutdownHook(new Thread(CommonEvent::nShutdown));
    }

    private static native Object nInitCoreCommon();

    private static native void nShutdown();
}
