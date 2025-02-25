package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TlmNetwork;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ReferenceFloatPair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class SubmitVariableChanges {
    public final int instanceId;
    public final List<ReferenceFloatPair<String>> variables;
    /**
     * 如果为 -1，则表示发送者自己
     */
    private final int entityId;

    public SubmitVariableChanges(int instanceId, List<ReferenceFloatPair<String>> variables, int entityId) {
        this.instanceId = instanceId;
        this.variables = variables;
        this.entityId = entityId;
    }

    public SubmitVariableChanges(int instanceId, List<ReferenceFloatPair<String>> variables) {
        this(instanceId, variables, -1);
    }

    public static void encode(SubmitVariableChanges message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.instanceId);
        buf.writeByte(message.variables.size());
        for (var variable : message.variables) {
            buf.writeUtf(variable.first());
            buf.writeFloat(variable.secondFloat());
        }
        buf.writeVarInt(message.entityId);
    }

    public static SubmitVariableChanges decode(FriendlyByteBuf buf) {
        int instanceId = buf.readVarInt();
        var variableSize = buf.readByte();
        List<ReferenceFloatPair<String>> variables = Lists.newArrayListWithCapacity(variableSize);
        for (var i = 0; i < variableSize; i++) {
            var key = buf.readUtf();
            var value = buf.readFloat();
            variables.add(ReferenceFloatPair.of(key, value));
        }
        int entityId = buf.readVarInt();
        return new SubmitVariableChanges(instanceId, variables, entityId);
    }

    public static void handle(final SubmitVariableChanges message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> handle(message, context));
        }
        context.setPacketHandled(true);
    }

    private static void handle(SubmitVariableChanges message, NetworkEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender == null) {
            return;
        }
        if (message.entityId != -1) {
            Entity entity = sender.serverLevel().getEntity(message.entityId);
            if (TlmNetwork.isMaid(entity)) {
                TlmNetwork.handleVariableChanges(entity, message);
            }
            return;
        }
        context.getSender().getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(cap -> {
            cap.updateVariables(message);
        });
    }
}
