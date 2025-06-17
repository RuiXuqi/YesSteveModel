package com.elfmcys.yesstevemodel.geckolib3.core.molang.storage;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.PooledStringHashMap;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.PooledStringHashSet;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MolangMemory implements IScopedVariableStorage, IForeignVariableStorage {
    private static final int SCOPED_INIT_CAPACITY = 16;

    private final StackMemory stackMemory = new StackMemory();
    private final PooledStringHashMap<VariableValueHolder> scopedMap = new PooledStringHashMap<>(SCOPED_INIT_CAPACITY);
    private PooledStringHashMap<VariableValueHolder> publicMap = new PooledStringHashMap<>();

    @Override
    public Object getScoped(int name) {
        VariableValueHolder valueHolder = scopedMap.computeIfAbsent(name, n -> new VariableValueHolder());
        return valueHolder.value;
    }

    @Override
    public void setScoped(int name, Object value) {
        VariableValueHolder valueHolder = scopedMap.computeIfAbsent(name, n -> new VariableValueHolder());
        valueHolder.value = value;
    }

    @Override
    public Object getPublic(int name) {
        VariableValueHolder valueHolder = publicMap.get(name);
        if(valueHolder != null) {
            return valueHolder.value;
        } else {
            return null;
        }
    }

    public StackMemory getStackMemory() {
        return stackMemory;
    }

    // 注意 this.publicMap 线程安全
    public void initialize(@Nullable PooledStringHashSet publicVariableNames) {
        scopedMap.clear();

        PooledStringHashMap<VariableValueHolder> newPublicMap = new PooledStringHashMap<>();
        if (publicVariableNames != null) {
            for (int publicVariableName : publicVariableNames) {
                VariableValueHolder value = new VariableValueHolder();
                scopedMap.put(publicVariableName, value);
                newPublicMap.put(publicVariableName, value);
            }
        }
        newPublicMap.trim();
        this.publicMap = newPublicMap;
    }

    public void visitScopedVariableNames(Consumer<String> visitor) {
        for (var name : scopedMap.keySet()) {
            visitor.accept(StringPool.getString(name));
        }
    }

    private static class VariableValueHolder {
        public Object value = null;
    }
}
