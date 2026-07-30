package com.elfmcys.ysm.client.event;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.client.model.ClientModelService;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.forge.ClientSessionRuntime;
import com.elfmcys.ysm.network.session.ActiveSessionMode;
import com.elfmcys.ysm.network.session.SessionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientLoggedEvent {
    private static boolean LOGGED_IN = false;

    @SubscribeEvent
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (LOGGED_IN) {
            return;
        }
        if (!YesSteveModel.isAvailable()) {
            LOGGED_IN = true;
            YesSteveModel.sendUnavailableMessage();
            return;
        }
        try {
            ClientModelService.instance().awaitBuiltinReadiness();
        } catch (RuntimeException error) {
            YesSteveModel.LOGGER.error(
                    "Builtin models were not ready before entering the level", error);
            event.getConnection().disconnect(Component.translatable(
                    "disconnect.yes_steve_model.builtin_initialization_failed"));
            return;
        }
        var connectionGeneration = ClientSessionRuntime.beginConnection();
        CompletableFuture.runAsync(
                () -> Minecraft.getInstance().execute(
                        () -> ClientSessionRuntime.completeGameServerDetection(connectionGeneration)),
                CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS));
        LOGGED_IN = true;

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
                var session = ClientSessionRuntime.snapshot();
                if (player != null && player.connection.isAcceptingMessages()
                        && session.requestedMode() == SessionMode.AUTO
                        && session.activeMode().orElse(null) == ActiveSessionMode.LOCAL_ONLY
                        && !NetworkHandler.isChannelPresent(player.connection.getConnection())) {
                    player.sendSystemMessage(Component.translatable("message.yes_steve_model.client.server_not_found"));
                }
            });
        });
        notifyThread.setDaemon(true);
        notifyThread.start();
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (!LOGGED_IN) {
            return;
        }
        LOGGED_IN = false;
        if (YesSteveModel.isAvailable()) {
            ClientSessionRuntime.disconnect();
        }
    }
}
