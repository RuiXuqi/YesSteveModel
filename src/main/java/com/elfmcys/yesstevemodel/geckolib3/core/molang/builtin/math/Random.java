package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.math;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.ContextFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;

public class Random extends ContextFunction<Object> {
    @Override
    public boolean validateArgumentSize(int size) {
        return size == 2 || size == 3;  // 出于兼容性考虑，允许 3 参数
    }

    @Override
    protected Object eval(ExecutionContext<IContext<Object>> context, ArgumentCollection arguments) {
        double min = arguments.getAsDouble(context, 0);
        double range = arguments.getAsDouble(context, 1);
        if(min > range) {
            double temp = min;
            min = range;
            range = temp - range;
        } else {
            range -= min;
        }
        return min + context.entity().random().nextDouble() * range;
    }
}
