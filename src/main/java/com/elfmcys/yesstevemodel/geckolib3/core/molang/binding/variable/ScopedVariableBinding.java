package com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.variable;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.molang.runtime.AssignableVariable;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ObjectBinding;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

import javax.annotation.Nonnull;

public class ScopedVariableBinding implements ObjectBinding {
    private final Int2ReferenceOpenHashMap<ScopedVariable> variableMap = new Int2ReferenceOpenHashMap<>();

    @Override
    public Object getProperty(String name) {
        return variableMap.computeIfAbsent(StringPool.computeIfAbsent(name), ScopedVariable::new);
    }

    public void reset() {
        variableMap.clear();
    }

    private static class ScopedVariable implements AssignableVariable {
        private final int name;

        private ScopedVariable(int name) {
            this.name = name;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object evaluate(final @Nonnull ExecutionContext<?> context) {
            return ((IContext<Object>) context.entity()).scopedStorage().getScoped(name);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void assign(@Nonnull ExecutionContext<?> context, Object value) {
            ((IContext<Object>) context.entity()).scopedStorage().setScoped(name, value);
        }
    }
}
