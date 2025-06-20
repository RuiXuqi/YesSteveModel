package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MolangSync {
    private final int entityId;
    private final FloatArrayList args;

    public MolangSync(int entityId, FloatArrayList args) {
        this.entityId = entityId;
        this.args = args;
    }

    public static void encode(MolangSync message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeByte(message.args.size());
        for (float arg : message.args) {
            buf.writeFloat(arg);
        }
    }

    public static MolangSync decode(FriendlyByteBuf buf) {
        var entityId = buf.readVarInt();
        var len = buf.readByte();
        var args = new FloatArrayList(len);
        for (int i = 0; i < len; i++) {
            args.add(buf.readFloat());
        }
        return new MolangSync(entityId, args);
    }

    @SuppressWarnings("DataFlowIssue")
    public static void handle(MolangSync message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> {
                var entity = Minecraft.getInstance().level.getEntity(message.entityId);
                entity.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                    cap.molangSync(message.args);
                });
            });
        }
        context.setPacketHandled(true);
    }
}
