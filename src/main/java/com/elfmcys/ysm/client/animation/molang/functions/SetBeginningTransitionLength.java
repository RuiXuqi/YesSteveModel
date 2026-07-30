package com.elfmcys.ysm.client.animation.molang.functions;

import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.function.ContextFunction;
import com.elfmcys.ysm.molang.runtime.ExecutionContext;

public class SetBeginningTransitionLength extends ContextFunction<Object> {
    @Override
    protected Object eval(ExecutionContext<IContext<Object>> ctx, ArgumentCollection arguments) {
        var controller = ctx.entity().animationEvent().getCodedController();
        if (controller == null) {
            return null;
        }
        var blendTime = arguments.getAsFloat(ctx, 0);
        if (blendTime < 0) {
            return null;
        }
        controller.setBeginningTransitionLength(blendTime);
        return null;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
