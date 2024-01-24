package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.AuthModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetModelAndTexture {
    private final ResourceLocation modelId;
    private final ResourceLocation selectTexture;

    public SetModelAndTexture(ResourceLocation modelId, ResourceLocation selectTexture) {
        this.modelId = modelId;
        this.selectTexture = selectTexture;
    }

    public static void encode(SetModelAndTexture message, FriendlyByteBuf buf) {
        buf.writeResourceLocation(message.modelId);
        buf.writeResourceLocation(message.selectTexture);
    }

    public static SetModelAndTexture decode(FriendlyByteBuf buf) {
        return new SetModelAndTexture(buf.readResourceLocation(), buf.readResourceLocation());
    }

    public static void handle(SetModelAndTexture message, Supplier<NetworkEvent.Context> contextSupplier) {
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

    private static void handleCapability(SetModelAndTexture message, ServerPlayer sender) {
        sender.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(modelIdCap -> sender.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(ownModelsCap -> {
            String modelName = message.modelId.getPath();
            if (!ServerModelManager.getModels().containsKey(modelName)
                    || ServerModelManager.getAuthModels().contains(modelName) && !ownModelsCap.containModel(message.modelId)
                    || !ServerModelManager.getModels().get(modelName).textures().contains(ModelIdUtil.getSubNameFromId(message.selectTexture)) ) {
                modelIdCap.setModelAndTexture(ModelIdUtil.DEFAULT_MODEL_ID, ModelIdUtil.DEFAULT_TEXTURE_ID);
            } else {
                modelIdCap.setModelAndTexture(message.modelId, message.selectTexture);
            }
        }));
    }
}
