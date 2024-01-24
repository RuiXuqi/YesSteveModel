package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

public class ClientLoggedOutEvent {
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientModelManager.syncAbort();
    }
}
