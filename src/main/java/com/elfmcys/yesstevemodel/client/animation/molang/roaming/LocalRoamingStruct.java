package com.elfmcys.yesstevemodel.client.animation.molang.roaming;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.molang.runtime.HashMapStruct;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ValueConversions;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.function.Consumer;

public class LocalRoamingStruct implements Struct {
    public final static int MAX_SIZE = 64;
    public final static int MAX_NAME_LENGTH = 32;

    private final Int2FloatOpenHashMap values;
    private final IntOpenHashSet names;
    private final int modelHashShort;
    private VariableChanges changes;
    private boolean dirty = false;

    public LocalRoamingStruct(int modelHashShort, Int2FloatOpenHashMap values) {
        this.changes = new VariableChanges(modelHashShort, 4);
        this.values = values;
        this.names = new IntOpenHashSet(values.keySet());
        this.modelHashShort = modelHashShort;
    }

    @Override
    public Object getProperty(int name) {
        return values.get(name);
    }

    @Override
    public void putProperty(int name, Object rawValue) {
        float value = ValueConversions.asFloat(rawValue);
        float oldValue = values.put(name, value);
        if (value == oldValue) {
            return;
        }

        names.add(name);
        if (names.size() > MAX_SIZE) {
            return;
        }

        changes.variables.put(name, value);
        dirty = true;
    }

    @Override
    public Struct copy() {
        HashMapStruct ret = new HashMapStruct(true);
        for (Int2FloatMap.Entry entry : values.int2FloatEntrySet()) {
            ret.putProperty(entry.getIntKey(), entry.getFloatValue());
        }
        return ret;
    }

    public boolean isDirty() {
        return dirty;
    }

    public VariableChanges popChanges() {
        var ret = changes;
        changes = new VariableChanges(modelHashShort, ret.variables.size());
        dirty = false;
        return ret;
    }

    public void visitNames(Consumer<String> nameVisitor) {
        for (var name : names) {
            var nameStr = StringPool.getString(name);
            if (nameStr != null) {
                nameVisitor.accept(nameStr);
            }
        }
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
