package com.elfmcys.yesstevemodel.client.animation.molang;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.molang.runtime.Array;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Variable;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public class UserFunctionArgument implements Array, Variable {
    public static final UserFunctionArgument INSTANCE = new UserFunctionArgument();

    @Override
    public @Nullable Object getElement(@NotNull ExecutionContext<?> context, int index) {
        if (context instanceof IContext<?> ctx && ctx.userFunctionArgs() != null) {
            return ctx.userFunctionArgs().getElement(context, index);
        }
        return null;
    }

    @Override
    public @Nullable Object evaluate(@NotNull ExecutionContext<?> context) {
        return getElement(context, 0);
    }

    @Override
    public Iterator<?> iterator(@NotNull ExecutionContext<?> context) {
        if (context instanceof IContext<?> ctx && ctx.userFunctionArgs() != null) {
            return ctx.userFunctionArgs().iterator(context);
        }
        return ObjectIterators.EMPTY_ITERATOR;
    }
}
