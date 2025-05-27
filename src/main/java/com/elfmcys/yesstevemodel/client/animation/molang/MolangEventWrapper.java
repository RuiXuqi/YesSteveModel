package com.elfmcys.yesstevemodel.client.animation.molang;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.runtime.Array;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import it.unimi.dsi.fastutil.floats.FloatIterators;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public class MolangEventWrapper {
    public static final int PLAYER_INIT = StringPool.computeIfAbsent("player_init");
    public static final int PLAYER_UPDATE = StringPool.computeIfAbsent("player_update");

    public static IValue wrap(List<IValue> handlers, float @Nullable [] args) {
        return wrap(handlers, args != null ? new FloatArgs(args) : Array.EMPTY);
    }

    public static IValue wrap(List<IValue> handlers, Object @Nullable [] args) {
        return wrap(handlers, args != null ? new GenericArgs(args) : Array.EMPTY);
    }

    public static IValue wrap(List<IValue> handlers) {
        return wrap(handlers, Array.EMPTY);
    }

    private static IValue wrap(List<IValue> handlers, Array array) {
        return evaluator -> {
            if (evaluator.entity() instanceof IContext<?> ctx) {
                for (var handler : handlers) {
                    return ctx.callUserFunction(evaluator, handler, array);
                }
            }
            return null;
        };
    }

    private static class FloatArgs implements Array {
        private final float[] args;

        public FloatArgs(float[] args) {
            this.args = args;
        }

        @Override
        public @Nullable Float getElement(@NotNull ExecutionContext<?> context, int index) {
            if (index < args.length) {
                return args[index];
            }
            return 0f;
        }

        @Override
        public Iterator<Float> iterator(@NotNull ExecutionContext<?> context) {
            return FloatIterators.wrap(args);
        }
    }

    private static class GenericArgs implements Array {
        private final Object[] args;

        public GenericArgs(Object[] args) {
            this.args = args;
        }

        @Override
        public @Nullable Object getElement(@NotNull ExecutionContext<?> context, int index) {
            if (index < args.length) {
                return args[index];
            }
            return 0f;
        }

        @Override
        public Iterator<Object> iterator(@NotNull ExecutionContext<?> context) {
            return ObjectIterators.wrap(args);
        }
    }
}
