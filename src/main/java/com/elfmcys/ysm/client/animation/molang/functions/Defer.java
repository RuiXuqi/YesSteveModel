package com.elfmcys.ysm.client.animation.molang.functions;

import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.function.ContextFunction;
import com.elfmcys.ysm.geckolib3.core.molang.util.StringPool;
import com.elfmcys.ysm.molang.runtime.ExecutionContext;

public class Defer extends ContextFunction<Object> {
    @Override
    protected Object eval(ExecutionContext<IContext<Object>> ctx, ArgumentCollection arguments) {
        if (ctx.entity().allowEmitting()) {
            var animCtx = ctx.entity().animationContext();
            if (animCtx != null) {
                var name = arguments.getAsPooledString(ctx, 0);
                if (name != StringPool.EMPTY) {
                    animCtx.defer(ctx, name, arguments, 1);
                }
            }
        }
        return null;
    }
}
