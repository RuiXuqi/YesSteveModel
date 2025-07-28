package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.ContextFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;

public class ResetController extends ContextFunction<Object> {
    @Override
    protected Object eval(ExecutionContext<IContext<Object>> ctx, ArgumentCollection arguments) {
        var controller = ctx.entity().animationEvent().getCodedController();
        if (controller == null) {
            return null;
        }

        controller.reset();
        return null;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size > 0;
    }
}
