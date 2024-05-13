package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientLoggedInEvent {
    @SubscribeEvent
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
            Minecraft.getInstance().execute(() -> {
                var connection = event.getPlayer().connection.getConnection();
                if (connection.isConnected() && !NetworkHandler.isChannelPresent(connection)) {
                    event.getPlayer().sendSystemMessage(Component.translatable("message.yes_steve_model.client.server_not_found"));
                }
            });
        }).start();
    }
}
