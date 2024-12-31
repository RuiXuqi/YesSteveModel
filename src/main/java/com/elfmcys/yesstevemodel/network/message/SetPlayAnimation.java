package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.info.ModelProperties;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;

public class SetPlayAnimation {
    private final int extraAnimationIndex;
    private final String classifyId;

    public SetPlayAnimation(int extraAnimationIndex, String classifyId) {
        this.extraAnimationIndex = extraAnimationIndex;
        this.classifyId = classifyId;
    }

    public static SetPlayAnimation stop() {
        return new SetPlayAnimation(-1, "");
    }

    public static void encode(SetPlayAnimation message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.extraAnimationIndex);
        buf.writeUtf(message.classifyId);
    }

    public static SetPlayAnimation decode(FriendlyByteBuf buf) {
        return new SetPlayAnimation(buf.readVarInt(), buf.readUtf());
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
                    ModelProperties properties = model.info().properties();
                    var classifyMap = properties.extraAnimationClassifyMap();
                    FifoHashMap<String, String> map;

                    if (StringUtils.isNotBlank(message.classifyId) && classifyMap.containsKey(message.classifyId)) {
                        map = classifyMap.get(message.classifyId);
                    } else {
                        map = properties.extraAnimationOrderMap();
                    }

                    if (map.size() > message.extraAnimationIndex) {
                        modelIdCap.playAnimation(map.getKeyAt(message.extraAnimationIndex));
                    }
                });
            }
        });
    }
}
