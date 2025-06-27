package com.elfmcys.yesstevemodel.client.animation.molang.roaming;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.molang.runtime.HashMapStruct;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ValueConversions;
import it.unimi.dsi.fastutil.ints.Int2FloatArrayMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

public class RemoteRoamingStruct implements Struct {
    private final Int2FloatOpenHashMap values;

    public RemoteRoamingStruct(Int2FloatOpenHashMap values) {
        this.values = values;
    }

    @Override
    public Object getProperty(int name) {
        return values.get(name);
    }

    @Override
    public void putProperty(int name, Object value) {
        values.put(name, ValueConversions.asFloat(value));
    }

    public void update(Int2FloatArrayMap changes) {
        values.putAll(changes);
    }

    @Override
    public Struct copy() {
        HashMapStruct ret = new HashMapStruct(true);
        for (Int2FloatMap.Entry entry : values.int2FloatEntrySet()) {
            ret.putProperty(entry.getIntKey(), entry.getFloatValue());
        }
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("roaming{");
        boolean first = true;
        for (Int2FloatMap.Entry entry : values.int2FloatEntrySet()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(String.format("%s=%s", StringPool.getString(entry.getIntKey()), entry.getFloatValue()));
        }
        builder.append("}");
        return builder.toString();
    }
}
