package com.elfmcys.yesstevemodel.geckolib3.core.molang.value;

import com.elfmcys.yesstevemodel.molang.parser.ast.Expression;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

import java.util.List;

public class MolangValue implements IValue {
    private final Expression[] expressions;

    public MolangValue(List<Expression> expressions) {
        this.expressions = expressions.toArray(new Expression[0]);
    }

    @Override
    public Object evalUnsafe(ExpressionEvaluator<?> evaluator) {
        Object lastResult = 0d;

        for (Expression expression : expressions) {
            lastResult = evaluator.evalUnsafe(expression);
            Object returnValue = evaluator.popReturnValue();
            if (returnValue != null) {
                lastResult = returnValue;
                break;
            }
        }

        return lastResult;
    }
}
