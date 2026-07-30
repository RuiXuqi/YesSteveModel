package com.elfmcys.ysm.network.message;

import com.elfmcys.ysm.capability.StarModelsCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetStarModel {
    private final String modelId;
    private final boolean isAdd;

    private SetStarModel(String modelId, boolean isAdd) {
        this.modelId = modelId;
        this.isAdd = isAdd;
    }

    public static SetStarModel add(String modelId) {
        return new SetStarModel(modelId, true);
    }

    public static SetStarModel remove(String modelId) {
        return new SetStarModel(modelId, false);
    }

    public static void encode(SetStarModel message, FriendlyByteBuf buf) {
        buf.writeUtf(message.modelId);
        buf.writeBoolean(message.isAdd);
    }

    public static SetStarModel decode(FriendlyByteBuf buf) {
        String modelId = buf.readUtf();
        boolean isAdd = buf.readBoolean();
        return new SetStarModel(modelId, isAdd);
    }

    public static void handle(SetStarModel message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null) {
                    return;
                }
                handleCapability(message, sender);
            });
        }
        context.setPacketHandled(true);
    }

    private static void handleCapability(SetStarModel message, ServerPlayer sender) {
        sender.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).ifPresent(cap -> {
            if (message.isAdd) {
                cap.addModel(message.modelId);
            } else {
                cap.removeModel(message.modelId);
            }
        });
    }
}
