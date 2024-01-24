package com.elfmcys.yesstevemodel.geckolib3.core.molang.value;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.molang.parser.ast.Expression;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ValueConversions;

import java.util.List;

public class MolangValue implements IValue {
    private final Expression[] expressions;

    public MolangValue(List<Expression> expressions) {
        this.expressions = expressions.toArray(new Expression[0]);
    }

    public double evalAsDouble(ExpressionEvaluator<?> evaluator) {
        try {
            Object result = evalUnsafe(evaluator);
            double value = ValueConversions.asDouble(result);
            if (!Double.isNaN(value)) {
                return value;
            }
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("Failed to evaluate molang value.", e);
        }
        return 0;
    }

    @Override
    public Object evalUnsafe(ExpressionEvaluator<?> evaluator) throws Exception {
        Object lastResult = 0d;

        for (Expression expression : expressions) {
            lastResult = evaluator.eval(expression);
            Object returnValue = evaluator.popReturnValue();
            if (returnValue != null) {
                lastResult = returnValue;
                break;
            }
        }

        return lastResult;
    }
}
