package com.elfmcys.ysm.network.message.data;

import com.elfmcys.ysm.geckolib3.core.molang.util.StringPool;
import it.unimi.dsi.fastutil.ints.Int2FloatArrayMap;
import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import net.minecraft.network.FriendlyByteBuf;

public class RoamingVarsChanges {
    public final int modelHashShort;
    /**
     * 如果为 -1，则表示发送者自己
     */
    public final int entityId;
    public final Object2FloatArrayMap<String> variablesServerBound;
    public final Int2FloatArrayMap variablesClientBound;

    public RoamingVarsChanges(int modelHashShort, Object2FloatArrayMap<String> variablesServerBound, Int2FloatArrayMap variablesClientBound, int entityId) {
        this.modelHashShort = modelHashShort;
        this.variablesServerBound = variablesServerBound;
        this.variablesClientBound = variablesClientBound;
        this.entityId = entityId;
    }

    public static void encode(RoamingVarsChanges message, FriendlyByteBuf buf) {
        buf.writeInt(message.modelHashShort);
        buf.writeVarInt(message.entityId);

        buf.writeByte(message.variablesServerBound.size());
        message.variablesServerBound.object2FloatEntrySet().fastForEach(entry -> {
            buf.writeUtf(entry.getKey());
            buf.writeFloat(entry.getFloatValue());
        });
    }

    public static RoamingVarsChanges decode(FriendlyByteBuf buf, boolean clientBound) {
        var modelHashShort = buf.readInt();
        var entityId = buf.readVarInt();

        var variableSize = buf.readByte();
        Object2FloatArrayMap<String> variablesServerBound;
        Int2FloatArrayMap variablesClientBound;
        if (clientBound) {
            var nameArray = new int[variableSize];
            var valueArray = new float[variableSize];

            for (var i = 0; i < variableSize; i++) {
                nameArray[i] = StringPool.computeIfAbsent(buf.readUtf());
                valueArray[i] = buf.readFloat();
            }
            variablesClientBound = new Int2FloatArrayMap(nameArray, valueArray);
            variablesServerBound = null;
        } else {
            var nameArray = new String[variableSize];
            var valueArray = new float[variableSize];

            for (var i = 0; i < variableSize; i++) {
                nameArray[i] = buf.readUtf();
                valueArray[i] = buf.readFloat();
            }
            variablesServerBound = new Object2FloatArrayMap<>(nameArray, valueArray);
            variablesClientBound = null;
        }

        return new RoamingVarsChanges(modelHashShort, variablesServerBound, variablesClientBound, entityId);
    }
}
