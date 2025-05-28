package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EmitMolangSync {
    private final FloatArrayList args;

    public EmitMolangSync(FloatArrayList args) {
        this.args = args;
    }

    public static void encode(EmitMolangSync message, FriendlyByteBuf buf) {
        buf.writeByte(message.args.size());
        for (float arg : message.args) {
            buf.writeFloat(arg);
        }
    }

    public static EmitMolangSync decode(FriendlyByteBuf buf) {
        var len = buf.readByte();
        var args = new FloatArrayList(len);
        for (int i = 0; i < len; i++) {
            args.add(buf.readFloat());
        }
        return new EmitMolangSync(args);
    }

    public static void handle(EmitMolangSync message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        var player = context.getSender();
        if (context.getDirection().getReceptionSide().isServer() && player != null) {
            context.enqueueWork(() -> {
                NetworkHandler.broadcastToVisiblePlayersAndSelf(new MolangSync(player.getId(), message.args), player);
            });
        }
        context.setPacketHandled(true);
    }
}
