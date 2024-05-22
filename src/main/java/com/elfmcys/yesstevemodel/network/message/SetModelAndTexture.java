package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.AuthModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetModelAndTexture {
    private final String modelId;
    private final String selectTexture;
    private final int instanceId;

    public SetModelAndTexture(String modelId, String selectTexture, int instanceId) {
        this.modelId = modelId;
        this.selectTexture = selectTexture;
        this.instanceId = instanceId;
    }

    public static void encode(SetModelAndTexture message, FriendlyByteBuf buf) {
        buf.writeUtf(message.modelId);
        buf.writeUtf(message.selectTexture);
        buf.writeVarInt(message.instanceId);
    }

    public static SetModelAndTexture decode(FriendlyByteBuf buf) {
        var modelId = buf.readUtf();
        var selectTexture = buf.readUtf();
        var instanceId = buf.readVarInt();
        return new SetModelAndTexture(modelId, selectTexture, instanceId);
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
                    || !ServerModelManager.getModels().get(modelId).textures().contains(message.selectTexture)) {
                modelIdCap.resetVariables(modelIdCap.getInstanceId() + 1);
                modelIdCap.setModelAndTexture(ModelIdUtil.DEFAULT_MODEL_ID, ModelIdUtil.DEFAULT_TEXTURE_NAME);
            } else {
                modelIdCap.resetVariables(message.instanceId);
                modelIdCap.setModelAndTexture(message.modelId, message.selectTexture);
            }
        }));
    }
}
