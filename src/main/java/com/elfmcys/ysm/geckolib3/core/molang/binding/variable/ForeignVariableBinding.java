package com.elfmcys.ysm.geckolib3.core.molang.binding.variable;

import com.elfmcys.ysm.geckolib3.core.molang.binding.ScopedObject;
import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.storage.IForeignVariableStorage;
import com.elfmcys.ysm.geckolib3.core.molang.util.StringPool;
import com.elfmcys.ysm.molang.runtime.ExecutionContext;
import com.elfmcys.ysm.molang.runtime.Variable;
import com.elfmcys.ysm.molang.runtime.binding.ObjectBinding;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

import org.jetbrains.annotations.NotNull;

public class ForeignVariableBinding implements ObjectBinding, ScopedObject {
    private final Int2ReferenceOpenHashMap<ForeignVariable> variableMap = new Int2ReferenceOpenHashMap<>();

    @Override
    public Object getProperty(String name) {
        return variableMap.computeIfAbsent(StringPool.computeIfAbsent(name), ForeignVariable::new);
    }

    public void resetScoped() {
        variableMap.clear();
    }

    private static class ForeignVariable implements Variable {
        private final int name;

        private ForeignVariable(int name) {
            this.name = name;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object evaluate(final @NotNull ExecutionContext<?> context) {
            IForeignVariableStorage storage = ((IContext<Object>) context.entity()).foreignStorage();
            if(storage != null) {
                return storage.getPublic(name);
            } else {
                return null;
            }
        }
    }
}
