package com.elfmcys.yesstevemodel.network.message.data;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
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
            variablesClientBound = new Int2FloatArrayMap(variableSize);
            variablesServerBound = null;
            for (var i = 0; i < variableSize; i++) {
                var key = StringPool.computeIfAbsent(buf.readUtf());
                var value = buf.readFloat();
                variablesClientBound.put(key, value);
            }
        } else {
            variablesServerBound = new Object2FloatArrayMap<>(variableSize);
            variablesClientBound = null;
            for (var i = 0; i < variableSize; i++) {
                var key = buf.readUtf();
                var value = buf.readFloat();
                variablesServerBound.put(key, value);
            }
        }

        return new RoamingVarsChanges(modelHashShort, variablesServerBound, variablesClientBound, entityId);
    }
}
