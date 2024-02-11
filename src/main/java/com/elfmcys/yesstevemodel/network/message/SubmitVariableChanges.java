package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ReferenceFloatPair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class SubmitVariableChanges {
    public final int instanceId;
    public final List<ReferenceFloatPair<String>> variables;

    public SubmitVariableChanges(int instanceId, List<ReferenceFloatPair<String>> variables) {
        this.instanceId = instanceId;
        this.variables = variables;
    }

    public static void encode(SubmitVariableChanges message, FriendlyByteBuf buf) {
        buf.writeInt(message.instanceId);
        buf.writeByte(message.variables.size());
        for (var variable : message.variables) {
            buf.writeUtf(variable.first());
            buf.writeFloat(variable.secondFloat());
        }
    }

    public static SubmitVariableChanges decode(FriendlyByteBuf buf) {
        int instanceId = buf.readInt();
        var variableSize = buf.readByte();
        List<ReferenceFloatPair<String>> variables = Lists.newArrayListWithCapacity(variableSize);
        for (var i = 0; i < variableSize; i++) {
            var key = buf.readUtf();
            var value = buf.readFloat();
            variables.add(ReferenceFloatPair.of(key, value));
        }

        return new SubmitVariableChanges(instanceId, variables);
    }

    public static void handle(final SubmitVariableChanges message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer() && context.getSender() != null) {
            context.enqueueWork(() -> {
                context.getSender().getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(cap -> {
                    cap.updateVariables(message);
                });
            });
        }
        context.setPacketHandled(true);
    }
}
