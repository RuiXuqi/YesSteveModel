package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod.EventBusSubscriber
public class ServerStartingEvent {
    @SubscribeEvent
    public static void onServerInit(final ServerAboutToStartEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientModelManager.setupDefaultModel();
        }
        ServerModelManager.reloadAndSync(result -> {
            // 虽然不太可能发生，但还是处理一下
            if (!result.success()) {
                event.getServer().execute(() -> {
                    throw new RuntimeException("YSM Loading Failed: " + result.message().getString(256));
                });
            }
        }, null);
    }
}
