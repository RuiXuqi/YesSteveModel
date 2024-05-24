package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.math;

import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;

public class Trunc implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        double value = arguments.getAsDouble(context, 0);
        return value < 0 ? Math.ceil(value) : Math.floor(value);
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
