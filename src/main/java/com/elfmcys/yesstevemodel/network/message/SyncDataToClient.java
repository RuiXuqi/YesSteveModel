package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

// 同步模型的协议由 Native 层定义和实现
// java 层只提供收发数据的接口，无需关注协议本身
public class SyncDataToClient {
    private final ByteBuffer data;

    public SyncDataToClient(ByteBuffer data) {
        this.data = data;
    }

    public static void encode(SyncDataToClient message, FriendlyByteBuf buf) {
        buf.writeBytes(message.data);
    }

    public static SyncDataToClient decode(FriendlyByteBuf buf) {
        ByteBuffer data = ByteBuffer.allocateDirect(buf.readableBytes());
        buf.readBytes(data);
        return new SyncDataToClient(data);
    }

    public static void handle(SyncDataToClient message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            ClientModelManager.syncReceiveData(context.getNetworkManager(), message.data);
        }
        context.setPacketHandled(true);
    }
}
