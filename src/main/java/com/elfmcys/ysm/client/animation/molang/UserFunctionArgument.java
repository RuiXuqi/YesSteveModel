package com.elfmcys.ysm.client.animation.molang;

import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.molang.runtime.ExecutionContext;
import com.elfmcys.ysm.molang.runtime.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserFunctionArgument implements Variable {
    public static final UserFunctionArgument INSTANCE = new UserFunctionArgument();

    @Override
    public @Nullable Object evaluate(@NotNull ExecutionContext<?> context) {
        if (context.entity() instanceof IContext<?> ctx) {
            return ctx.userFunctionArgs();
        }
        return null;
    }
}
