package com.elfmcys.ysm.event;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.model.ServerModelManager;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ServerStartingEvent {
    @SubscribeEvent
    public static void onServerInit(final ServerAboutToStartEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
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
