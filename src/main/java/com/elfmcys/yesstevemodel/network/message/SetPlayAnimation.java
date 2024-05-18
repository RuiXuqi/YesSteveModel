package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetPlayAnimation {
    private final int extraAnimationIndex;

    public SetPlayAnimation(int extraAnimationIndex) {
        this.extraAnimationIndex = extraAnimationIndex;
    }

    public static SetPlayAnimation stop() {
        return new SetPlayAnimation(-1);
    }

    public static void encode(SetPlayAnimation message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.extraAnimationIndex);
    }

    public static SetPlayAnimation decode(FriendlyByteBuf buf) {
        return new SetPlayAnimation(buf.readVarInt());
    }

    public static void handle(SetPlayAnimation message, Supplier<NetworkEvent.Context> contextSupplier) {
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

    private static void handleCapability(SetPlayAnimation message, ServerPlayer sender) {
        sender.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(modelIdCap -> {
            if (message.extraAnimationIndex == -1) {
                modelIdCap.stopAnimation();
            } else {
                ServerModelManager.getModel(modelIdCap.getModelId()).ifPresent(model -> {
                    if (model.info().properties().extraAnimationOrderMap().size() > message.extraAnimationIndex) {
                        modelIdCap.playAnimation(model.info().properties().extraAnimationOrderMap().getKeyAt(message.extraAnimationIndex));
                    }
                });
            }
        });
    }
}
