package com.elfmcys.ysm.network.message;

import com.elfmcys.ysm.model.ServerModelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

// 同步模型的协议由 Native 层定义和实现
// java 层只提供收发数据的接口，无需关注协议本身
public class SyncDataToServer {
    private final ByteBuffer data;

    public SyncDataToServer(ByteBuffer data) {
        this.data = data;
    }

    public static void encode(SyncDataToServer message, FriendlyByteBuf buf) {
        buf.writeBytes(message.data);
    }

    public static SyncDataToServer decode(FriendlyByteBuf buf) {
        ByteBuffer data = ByteBuffer.allocateDirect(buf.readableBytes());
        buf.readBytes(data);
        return new SyncDataToServer(data);
    }

    public static void handle(SyncDataToServer message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer() && context.getSender() != null) {
            ServerModelManager.syncReceiveData(context.getSender().getUUID(), message.data);
        }
        context.setPacketHandled(true);
    }
}
