package com.elfmcys.ysm.network.message;

import com.elfmcys.ysm.client.ClientModelManager;
import com.elfmcys.ysm.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerInfo {
    private final String channelVersion;

    public ServerInfo() {
        this(NetworkHandler.VERSION);
    }

    private ServerInfo(String channelVersion) {
        this.channelVersion = channelVersion;
    }

    public static ServerInfo decode(FriendlyByteBuf friendlyByteBuf) {
        String channelVersion = friendlyByteBuf.readUtf();
        return new ServerInfo(channelVersion);
    }

    public static void encode(ServerInfo serverInfo, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeUtf(serverInfo.channelVersion);
    }

    public static void handleOnClient(ServerInfo serverInfo, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (NetworkHandler.setChannelVersion(context.getNetworkManager(), serverInfo.channelVersion)) {
            context.enqueueWork(() -> {
                ClientModelManager.receiveServerInfo();
            });
        }
        NetworkHandler.CHANNEL.reply(new ClientInfo(), context);
        context.setPacketHandled(true);
    }
}
