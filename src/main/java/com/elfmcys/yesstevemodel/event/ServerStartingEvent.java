package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.model.ServerModelManager;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ServerStartingEvent {
    @SubscribeEvent
    public static void onServerInit(final ServerAboutToStartEvent event) {
        ServerModelManager.reloadAndSync(result -> {
            // 虽然不太可能发生，但还是处理一下
            if (!result.success()) {
                event.getServer().execute(() -> {
                    throw new ReportedException(new CrashReport("YSM Loading Failed,",
                            new RuntimeException(result.message().getString())));
                });
            }
        }, null);
    }
}
