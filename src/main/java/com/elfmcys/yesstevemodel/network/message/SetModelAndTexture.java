package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.AuthModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetModelAndTexture {
    private final String modelId;
    private final String selectTexture;

    public SetModelAndTexture(String modelId, String selectTexture) {
        this.modelId = modelId;
        this.selectTexture = selectTexture;
    }

    public static void encode(SetModelAndTexture message, FriendlyByteBuf buf) {
        buf.writeUtf(message.modelId);
        buf.writeUtf(message.selectTexture);
    }

    public static SetModelAndTexture decode(FriendlyByteBuf buf) {
        var modelId = buf.readUtf();
        var selectTexture = buf.readUtf();
        return new SetModelAndTexture(modelId, selectTexture);
    }

    public static void handle(SetModelAndTexture message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null) {
                    return;
                }
                if (ServerConfig.CAN_SWITCH_MODEL.get()) {
                    handleCapability(message, sender);
                }
            });
        }
        context.setPacketHandled(true);
    }

    private static void handleCapability(SetModelAndTexture message, ServerPlayer sender) {
        sender.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(modelIdCap -> sender.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(ownModelsCap -> {
            String modelId = message.modelId;
            if (!ServerModelManager.getModels().containsKey(modelId)
                    || (ServerModelManager.getAuthModels().contains(modelId) && !ownModelsCap.containModel(message.modelId))
                    || !ServerModelManager.getModels().get(modelId).playerModel().textures().contains(message.selectTexture)) {
                modelIdCap.setDefault();
            } else {
                modelIdCap.setModelAndTexture(message.modelId, message.selectTexture);
            }
            modelIdCap.stopAnimation();
        }));
    }
}
