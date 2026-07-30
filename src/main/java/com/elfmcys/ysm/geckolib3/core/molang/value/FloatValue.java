package com.elfmcys.ysm.geckolib3.core.molang.value;

import com.elfmcys.ysm.molang.runtime.ExpressionEvaluator;

public class FloatValue implements IValue {
    public static final FloatValue ONE = new FloatValue(1);
    public static final FloatValue ZERO = new FloatValue(0);

    private final float value;

    public FloatValue(float value) {
        if (!Float.isNaN(value)) {
            this.value = value;
        } else {
            this.value = 0;
        }
    }

    @Override
    public float evalAsFloat(ExpressionEvaluator<?> evaluator) {
        return value;
    }

    @Override
    public boolean evalAsBoolean(ExpressionEvaluator<?> evaluator) {
        return value != 0;
    }

    @Override
    public Object eval(ExpressionEvaluator<?> evaluator) {
        return value;
    }

    @Override
    public Object evalUnsafe(ExpressionEvaluator<?> evaluator) {
        return value;
    }

    public float value() {
        return value;
    }
}
