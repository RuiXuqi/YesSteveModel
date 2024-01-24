package com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.math.functions;

import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;

public class Cos implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        return Math.cos(arguments.getAsDouble(context, 0) / 180 * Math.PI);
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
