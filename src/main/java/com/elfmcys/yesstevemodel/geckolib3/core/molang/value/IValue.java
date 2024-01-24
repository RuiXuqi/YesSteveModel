package com.elfmcys.yesstevemodel.geckolib3.core.molang.value;

import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

public interface IValue {
    /**
     * 依次执行表达式，返回最后一个表达式的值，或第一个 return 语句的值，并转换为 double 类型。
     */
    double evalAsDouble(ExpressionEvaluator<?> evaluator);

    /**
     * 依次执行表达式，返回最后一个表达式的值，或第一个 return 语句的值，返回值的类型不确定，并且可能抛出异常。
     */
    Object evalUnsafe(ExpressionEvaluator<?> evaluator) throws Exception;
}
