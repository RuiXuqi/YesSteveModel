package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SubmitRouletteConfig {
    private final String molangExpression;

    public SubmitRouletteConfig(String molangExpression) {
        this.molangExpression = molangExpression;
    }

    public static void encode(SubmitRouletteConfig message, FriendlyByteBuf buf) {
        buf.writeUtf(message.molangExpression);
    }

    public static SubmitRouletteConfig decode(FriendlyByteBuf buf) {
        String molangString = buf.readUtf();
        return new SubmitRouletteConfig(molangString);
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
            ExecuteMolang executeMolang = new ExecuteMolang(sender.getId(), message.molangExpression);
            NetworkHandler.broadcastToVisiblePlayers(executeMolang, sender);
        }
    }
}
