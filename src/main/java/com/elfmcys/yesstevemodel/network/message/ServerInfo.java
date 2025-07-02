package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.config.DisableSwitch;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerInfo {
    private final String channelVersion;
    private final boolean canSwitchModel;

    public ServerInfo() {
        this(NetworkHandler.VERSION, ServerConfig.CAN_SWITCH_MODEL.get());
    }

    private ServerInfo(String channelVersion, boolean canSwitchModel) {
        this.channelVersion = channelVersion;
        this.canSwitchModel = canSwitchModel;
    }

    public static ServerInfo decode(FriendlyByteBuf friendlyByteBuf) {
        String channelVersion = friendlyByteBuf.readUtf();
        boolean canSwitchModel = friendlyByteBuf.readBoolean();
        return new ServerInfo(channelVersion, canSwitchModel);
    }

    public static void encode(ServerInfo serverInfo, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeUtf(serverInfo.channelVersion);
        friendlyByteBuf.writeBoolean(serverInfo.canSwitchModel);
    }

    public static void handleOnClient(ServerInfo serverInfo, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (NetworkHandler.setChannelVersion(context.getNetworkManager(), serverInfo.channelVersion)) {
            context.enqueueWork(() -> {
                DisableSwitch.CAN_SWITCH = serverInfo.canSwitchModel;
                ClientModelManager.receiveServerInfo();
            });
        }
        NetworkHandler.CHANNEL.reply(new ClientInfo(), context);
        context.setPacketHandled(true);
    }
}
