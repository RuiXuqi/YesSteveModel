package com.elfmcys.yesstevemodel.geckolib3.core.molang.storage;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.PooledStringHashMap;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.PooledStringHashSet;
import com.elfmcys.yesstevemodel.molang.runtime.Array;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import org.jetbrains.annotations.Nullable;

public class MolangMemory implements ITempVariableStorage, IScopedVariableStorage, IForeignVariableStorage {
    private static final int TEMP_INIT_CAPACITY = 16;
    private static final int SCOPED_INIT_CAPACITY = 16;
    private static final int MAX_STACK_DEPTH = 16;

    private final ReferenceArrayList<Object> tempStackFrame = new ReferenceArrayList<>(TEMP_INIT_CAPACITY);
    private final IntArrayList stackFrameSize = new IntArrayList();
    private final ReferenceArrayList<Array> userFunctionArgs = new ReferenceArrayList<>(4);

    private final PooledStringHashMap<VariableValueHolder> scopedMap = new PooledStringHashMap<>(SCOPED_INIT_CAPACITY);
    private PooledStringHashMap<VariableValueHolder> publicMap = new PooledStringHashMap<>();

    public MolangMemory() {
        stackFrameSize.add(0);
    }

    @Override
    public Object getTemp(int index) {
        var addr = stackFrameSize.getInt(stackFrameSize.size() - 1) + index;
        tempStackFrame.size(addr + 1);
        return tempStackFrame.elements()[addr];
    }

    @Override
    public void setTemp(int index, Object value) {
        var addr = stackFrameSize.getInt(stackFrameSize.size() - 1) + index;
        tempStackFrame.size(addr + 1);
        tempStackFrame.elements()[addr] = value;
    }

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

    // 注意 this.publicMap 线程安全
    public void initialize(@Nullable PooledStringHashSet publicVariableNames) {
        tempStackFrame.size(0);
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

    public boolean pushUserFunctionStackFrame(Array args) {
        if (stackFrameSize.size() < MAX_STACK_DEPTH) {
            stackFrameSize.add(tempStackFrame.size());
            userFunctionArgs.add(args);
            return true;
        } else {
            return false;
        }
    }

    public void popUserFunctionStackFrame() {
        if (!userFunctionArgs.isEmpty()) {
            var lastFrameSize = stackFrameSize.removeInt(tempStackFrame.size() - 1);
            tempStackFrame.size(lastFrameSize);
            userFunctionArgs.remove(userFunctionArgs.size() - 1);
        }
    }

    @Nullable
    public Array getUserFunctionArgs() {
        if (!userFunctionArgs.isEmpty()) {
            return userFunctionArgs.get(userFunctionArgs.size() - 1);
        }
        return null;
    }

    private static class VariableValueHolder {
        public Object value = null;
    }
}
