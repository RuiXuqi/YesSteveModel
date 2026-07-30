package com.elfmcys.ysm.geckolib3.core.molang.builtin.math;

import com.elfmcys.ysm.molang.runtime.ExecutionContext;
import com.elfmcys.ysm.molang.runtime.Function;

public class Ln implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        return Math.log(arguments.getAsDouble(context, 0));
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
