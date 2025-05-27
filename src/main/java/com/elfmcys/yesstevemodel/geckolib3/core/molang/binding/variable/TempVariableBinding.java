package com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.variable;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.TransientObject;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.molang.runtime.AssignableVariable;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ObjectBinding;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import org.jetbrains.annotations.NotNull;

public class TempVariableBinding implements ObjectBinding, TransientObject {
    private final Object2ReferenceMap<String, TempVariable> variableMap = new Object2ReferenceOpenHashMap<>();
    private int topPointer = 0;

    @Override
    public Object getProperty(String name) {
        return variableMap.computeIfAbsent(name, k -> new TempVariable(topPointer++));
    }

    public void resetTransient() {
        variableMap.clear();
        topPointer = 0;
    }

    private static class TempVariable implements AssignableVariable {
        private final int address;

        private TempVariable(int address) {
            this.address = address;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object evaluate(final @NotNull ExecutionContext<?> context) {
            return ((IContext<Object>) context.entity()).tempStorage().getTemp(address);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void assign(@NotNull ExecutionContext<?> context, Object value) {
            ((IContext<Object>) context.entity()).tempStorage().setTemp(address, value);
        }
    }
}
