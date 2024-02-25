package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class ServerInfoPacket implements IntSupplier {
    private final String channelVersion;
    private int loginIndex;

    @SuppressWarnings("unused")
    public ServerInfoPacket() {
        this(NetworkHandler.VERSION);
    }

    private ServerInfoPacket(String channelVersion) {
        this.channelVersion = channelVersion;
    }

    public void setLoginIndex(final int loginIndex) {
        this.loginIndex = loginIndex;
    }

    public int getLoginIndex() {
        return loginIndex;
    }

    @Override
    public int getAsInt() {
        return getLoginIndex();
    }

    public static ServerInfoPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new ServerInfoPacket(friendlyByteBuf.readUtf());
    }

    public static void encode(ServerInfoPacket serverInfoPacket, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeUtf(serverInfoPacket.channelVersion);
    }

    public static void handleOnClient(HandshakeHandler handshakeHandler, ServerInfoPacket serverInfoPacket, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        NetworkHandler.setChannelVersion(context.getNetworkManager(), serverInfoPacket.channelVersion);
        NetworkHandler.CHANNEL.reply(new ClientInfoPacket(), context);
        context.setPacketHandled(true);
    }
}
