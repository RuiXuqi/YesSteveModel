package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SubmitRouletteConfig {
    private final String molangExpression;
    private final int entityId;

    public SubmitRouletteConfig(String molangExpression, int entityId) {
        this.molangExpression = molangExpression;
        this.entityId = entityId;
    }

    public static void encode(SubmitRouletteConfig message, FriendlyByteBuf buf) {
        buf.writeUtf(message.molangExpression);
        buf.writeVarInt(message.entityId);
    }

    public static SubmitRouletteConfig decode(FriendlyByteBuf buf) {
        String molangString = buf.readUtf();
        int entityId = buf.readVarInt();
        return new SubmitRouletteConfig(molangString, entityId);
    }

    public static void handle(SubmitRouletteConfig message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> handlePacket(message, contextSupplier));
        }
        context.setPacketHandled(true);
    }

    private static void handlePacket(final SubmitRouletteConfig message, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender != null && sender.isAlive()) {
            Entity entity = sender.level().getEntity(message.entityId);
            if (entity == null) {
                return;
            }
            ExecuteMolang executeMolang = new ExecuteMolang(message.entityId, message.molangExpression);
            NetworkHandler.broadcastToVisiblePlayers(executeMolang, entity);
        }
    }
}
