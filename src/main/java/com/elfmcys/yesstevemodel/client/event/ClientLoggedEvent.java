package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientLoggedEvent {
    private static boolean LOGGED_IN = false;

    @SubscribeEvent
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (LOGGED_IN) {
            return;
        }
        LOGGED_IN = true;
        if (!YesSteveModel.isAvailable()) {
            YesSteveModel.sendUnavailableMessage();
            return;
        }

        if (Minecraft.getInstance().isLocalServer()) {
            return;
        }
        var notifyThread = new Thread(() -> {
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ignored) {
                return;
            }
            Minecraft.getInstance().execute(() -> {
                var player = Minecraft.getInstance().player;
                if (player != null && player.connection.isAcceptingMessages() && !NetworkHandler.isChannelPresent(player.connection.getConnection())) {
                    player.sendSystemMessage(Component.translatable("message.yes_steve_model.client.server_not_found"));
                }
            });
        });
        notifyThread.setDaemon(true);
        notifyThread.start();
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (LOGGED_IN) {
            LOGGED_IN = false;
            if (!YesSteveModel.isAvailable()) {
                return;
            }

            ClientModelManager.syncAbort();
        }
    }
}
