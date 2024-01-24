package com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.math.functions;

import com.elfmcys.yesstevemodel.mclib.utils.Interpolations;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;

public class Lerp implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        return Interpolations.lerp(arguments.getAsDouble(context, 0),
                arguments.getAsDouble(context, 1),
                arguments.getAsDouble(context, 2));
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 3;
    }
}
