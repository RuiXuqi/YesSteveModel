package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.config.DisableSwitch;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerInfoPacket {
    private final String channelVersion;
    private final boolean canSwitchModel;

    public ServerInfoPacket() {
        this(NetworkHandler.VERSION, ServerConfig.CAN_SWITCH_MODEL.get());
    }

    private ServerInfoPacket(String channelVersion, boolean canSwitchModel) {
        this.channelVersion = channelVersion;
        this.canSwitchModel = canSwitchModel;
    }

    public static ServerInfoPacket decode(FriendlyByteBuf friendlyByteBuf) {
        String channelVersion = friendlyByteBuf.readUtf();
        boolean canSwitchModel = friendlyByteBuf.readBoolean();
        return new ServerInfoPacket(channelVersion, canSwitchModel);
    }

    public static void encode(ServerInfoPacket serverInfoPacket, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeUtf(serverInfoPacket.channelVersion);
        friendlyByteBuf.writeBoolean(serverInfoPacket.canSwitchModel);
    }

    public static void handleOnClient(ServerInfoPacket serverInfoPacket, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        NetworkHandler.setChannelVersion(context.getNetworkManager(), serverInfoPacket.channelVersion);
        context.enqueueWork(() -> DisableSwitch.CAN_SWITCH = serverInfoPacket.canSwitchModel);
        NetworkHandler.CHANNEL.reply(new ClientInfoPacket(), context);
        context.setPacketHandled(true);
    }
}
