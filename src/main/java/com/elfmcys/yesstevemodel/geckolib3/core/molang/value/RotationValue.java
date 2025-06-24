package com.elfmcys.yesstevemodel.geckolib3.core.molang.value;

import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

public class RotationValue implements IValue {
    private final IValue value;
    private final boolean flip;

    public RotationValue(IValue value, boolean flip) {
        this.value = value;
        this.flip = flip;
    }

    public static float processValue(float value, boolean flip) {
        float ret = (float) Math.toRadians(value);
        if(flip) {
            return -ret;
        }
        return ret;
    }

    @Override
    public float evalAsFloat(ExpressionEvaluator<?> evaluator) {
        return processValue(this.value.evalAsFloat(evaluator), this.flip);
    }

    @Override
    public Object eval(ExpressionEvaluator<?> evaluator) {
        return evalAsFloat(evaluator);
    }

    @Override
    public Object evalUnsafe(ExpressionEvaluator<?> evaluator) {
        return evalAsFloat(evaluator);
    }
}
