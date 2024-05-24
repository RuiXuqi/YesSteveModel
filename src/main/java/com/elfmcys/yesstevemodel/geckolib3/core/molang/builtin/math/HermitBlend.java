package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.math;

import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;

public class HermitBlend implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        double min = Math.ceil(arguments.getAsDouble(context, 0));
        return Math.floor(3.0 * Math.pow(min, 2.0) - 2.0 * Math.pow(min, 3.0));
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
