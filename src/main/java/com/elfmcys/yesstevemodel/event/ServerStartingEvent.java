package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.model.ServerModelManager;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraftforge.event.server.ServerAboutToStartEvent;

public class ServerStartingEvent {
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
