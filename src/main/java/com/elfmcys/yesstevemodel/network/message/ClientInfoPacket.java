package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientInfoPacket {
    private final String channelVersion;

    public ClientInfoPacket() {
        this(NetworkHandler.VERSION);
    }

    public ClientInfoPacket(String channelVersion) {
        this.channelVersion = channelVersion;
    }

    public static ClientInfoPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new ClientInfoPacket(friendlyByteBuf.readUtf());
    }

    public static void encode(ClientInfoPacket clientInfoPacket, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeUtf(clientInfoPacket.channelVersion);
    }

    public static void handleOnServer(ClientInfoPacket clientInfoPacket, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && NetworkHandler.setChannelVersion(context.getNetworkManager(), clientInfoPacket.channelVersion)
                && NetworkHandler.isChannelPresent(context.getNetworkManager())) {
            ServerModelManager.syncModelsToPlayer(player, null);
        }
        context.setPacketHandled(true);
    }
}
