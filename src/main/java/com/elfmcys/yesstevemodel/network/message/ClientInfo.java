package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.AuthModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.StarModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientInfo {
    private final String channelVersion;

    public ClientInfo() {
        this(NetworkHandler.VERSION);
    }

    public ClientInfo(String channelVersion) {
        this.channelVersion = channelVersion;
    }

    public static ClientInfo decode(FriendlyByteBuf friendlyByteBuf) {
        return new ClientInfo(friendlyByteBuf.readUtf());
    }

    public static void encode(ClientInfo clientInfo, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeUtf(clientInfo.channelVersion);
    }

    public static void handleOnServer(ClientInfo clientInfo, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && NetworkHandler.setChannelVersion(context.getNetworkManager(), clientInfo.channelVersion)
                && NetworkHandler.isChannelPresent(context.getNetworkManager())) {
            player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(cap -> {
                cap.setMandatory(false);
                cap.stopAnimation();
            });
            player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(cap -> {
                NetworkHandler.sendToClientPlayer(new SyncAuthModels(cap.getAuthModels()), player);
            });
            player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).ifPresent(cap -> {
                NetworkHandler.sendToClientPlayer(new SyncStarModels(cap.getStarModels()), player);
            });
            ServerModelManager.syncModelsToPlayer(player, null);
        }
        context.setPacketHandled(true);
    }
}
