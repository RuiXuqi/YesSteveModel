package com.elfmcys.yesstevemodel.client.animation.molang;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ScopedObject;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.runtime.*;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ObjectBinding;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserFunctionBinding implements ObjectBinding, ScopedObject {
    private final Int2ObjectOpenHashMap<UserFunction> funcCache = new Int2ObjectOpenHashMap<>();

    @Override
    public Function getProperty(String name) {
        return funcCache.computeIfAbsent(StringPool.computeIfAbsent(name), UserFunction::new);
    }

    public void resetScoped() {
        funcCache.clear();
    }

    private static class UserFunction implements Function, Variable {
        private int name;
        private IValue cache;

        private UserFunction(int name) {
            this.name = name;
        }

        @Override
        public @Nullable Object evaluate(@NotNull ExecutionContext<?> context, @NotNull ArgumentCollection arguments) {
            if (context.entity() instanceof IContext<?> ctx) {
                if (cache == null) {
                    if (name == Integer.MIN_VALUE) {
                        return null;
                    }
                    cache = ctx.getUserFunction(name);
                    if (cache == null) {
                        ctx.debugPrint("User function not found: %s", StringPool.getString(name));
                        name = Integer.MIN_VALUE;
                        return null;
                    }
                }

                var args = new ReferenceArrayList<>(arguments.size());
                for (int i = 0; i < arguments.size(); i++) {
                    args.add(arguments.getValue(context, i));
                }
                return ctx.callUserFunction(context, cache, args);
            }
            return null;
        }

        @Override
        public @Nullable Object evaluate(@NotNull ExecutionContext<?> context) {
            return evaluate(context, EMPTY_ARGUMENT);
        }
    }
}
