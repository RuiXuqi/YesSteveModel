package com.elfmcys.ysm.geckolib3.core.molang.value;

import com.elfmcys.ysm.molang.parser.ast.Expression;
import com.elfmcys.ysm.molang.runtime.ExpressionEvaluator;

import java.util.List;

public class MolangValue implements IValue {
    private final List<Expression> expressions;
    private final boolean isUserFunc;

    public MolangValue(List<Expression> expressions , boolean isUserFunc) {
        this.expressions = expressions;
        this.isUserFunc = isUserFunc;
    }

    @Override
    public Object evalUnsafe(ExpressionEvaluator<?> evaluator) {
        return evaluator.evalMultiExpressionUnsafe(expressions, isUserFunc);
    }
}
