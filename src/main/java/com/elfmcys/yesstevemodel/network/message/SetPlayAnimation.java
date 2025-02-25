package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TlmNetwork;
import com.elfmcys.yesstevemodel.info.ModelProperties;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;

public class SetPlayAnimation {
    private final int extraAnimationIndex;
    private final String classifyId;
    /**
     * 想要播放轮盘动画的实体对象 ID，如果为 -1，则表示发送者自己
     */
    private final int entityId;

    public SetPlayAnimation(int extraAnimationIndex, String classifyId, int entityId) {
        this.extraAnimationIndex = extraAnimationIndex;
        this.classifyId = classifyId;
        this.entityId = entityId;
    }

    public SetPlayAnimation(int extraAnimationIndex, String classifyId) {
        this(extraAnimationIndex, classifyId, -1);
    }

    public static SetPlayAnimation stop() {
        return new SetPlayAnimation(-1, "");
    }

    public static SetPlayAnimation stop(int entityId) {
        return new SetPlayAnimation(-1, "", entityId);
    }

    public static void encode(SetPlayAnimation message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.extraAnimationIndex);
        buf.writeUtf(message.classifyId);
        buf.writeVarInt(message.entityId);
    }

    public static SetPlayAnimation decode(FriendlyByteBuf buf) {
        return new SetPlayAnimation(buf.readVarInt(), buf.readUtf(), buf.readVarInt());
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
        if (message.entityId != -1) {
            Entity entity = sender.serverLevel().getEntity(message.entityId);
            if (TlmNetwork.isMaid(entity)) {
                TlmNetwork.setRouletteAnim(entity, message.classifyId, message.extraAnimationIndex);
            }
            return;
        }

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
