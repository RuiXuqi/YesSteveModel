package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class ClientInfoPacket implements IntSupplier {
    private final String channelVersion;
    private int loginIndex;

    @SuppressWarnings("unused")
    public ClientInfoPacket() {
        this(NetworkHandler.VERSION);
    }

    public ClientInfoPacket(String channelVersion) {
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

    public static ClientInfoPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new ClientInfoPacket(friendlyByteBuf.readUtf());
    }

    public static void encode(ClientInfoPacket clientInfoPacket, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeUtf(clientInfoPacket.channelVersion);
    }

    public static void handleOnServer(HandshakeHandler handshakeHandler, ClientInfoPacket clientInfoPacket, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        NetworkHandler.setChannelVersion(context.getNetworkManager(), clientInfoPacket.channelVersion);
        context.setPacketHandled(true);
    }
}
