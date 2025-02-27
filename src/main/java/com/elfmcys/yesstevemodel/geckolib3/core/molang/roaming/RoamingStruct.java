package com.elfmcys.yesstevemodel.geckolib3.core.molang.roaming;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.molang.runtime.HashMapStruct;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ValueConversions;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class RoamingStruct implements Struct {
    private final static int MAX_SIZE = 16;
    private final static int MAX_NAME_LENGTH = 16;

    private final Int2FloatOpenHashMap values = new Int2FloatOpenHashMap();
    private final ObjectOpenHashSet<String> names = new ObjectOpenHashSet<>();
    private int instanceId;
    private volatile VariableChanges changes;
    private volatile boolean dirty = false;

    public RoamingStruct() {
        changes = new VariableChanges(instanceId);
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

        String nameStr = StringPool.getString(name);
        if (nameStr.length() > MAX_NAME_LENGTH) {
            return;
        }
        names.add(nameStr);
        if (names.size() > MAX_SIZE) {
            return;
        }

        if (changes.instanceId == instanceId) {
            changes.variables.put(nameStr, value);
            dirty = true;
        }
    }

    @Override
    public Struct copy() {
        HashMapStruct ret = new HashMapStruct(true);
        for (Int2FloatMap.Entry entry : values.int2FloatEntrySet()) {
            ret.putProperty(entry.getIntKey(), entry.getFloatValue());
        }
        return ret;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public void reset(int instanceId, @Nullable Object2FloatOpenHashMap<String> initialVariables) {
        if (changes.instanceId != instanceId) {
            values.clear();
            names.clear();
        }
        this.instanceId = instanceId;
        if (initialVariables != null) {
            for (var entry : initialVariables.object2FloatEntrySet()) {
                putProperty(StringPool.computeIfAbsent(entry.getKey()), entry.getFloatValue());
            }
        }
        if (changes.instanceId != instanceId) {
            popChanges();
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public VariableChanges popChanges() {
        var ret = changes;
        changes = new VariableChanges(instanceId);
        dirty = false;
        return ret;
    }

    public Set<String> getAllName() {
        return names;
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
