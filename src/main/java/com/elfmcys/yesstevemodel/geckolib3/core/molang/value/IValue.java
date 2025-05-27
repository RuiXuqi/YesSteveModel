package com.elfmcys.yesstevemodel.geckolib3.core.molang.value;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ValueConversions;

public interface IValue {
    /**
     * 依次执行表达式，返回最后一个表达式的值，或第一个 return 语句的值，并转换为 double 类型。
     */
    default double evalAsDouble(ExpressionEvaluator<?> evaluator) {
        try {
            Object result = evalUnsafe(evaluator);
            double value = ValueConversions.asDouble(result);
            if (!Double.isNaN(value)) {
                return value;
            }
        } catch (Exception e) {
            YesSteveModel.LOGGER.debug("Failed to evaluate molang expression.", e);
        }
        return 0;
    }

    /**
     * 依次执行表达式，返回最后一个表达式的值，或第一个 return 语句的值，并转换为 boolean 类型。
     */
    default boolean evalAsBoolean(ExpressionEvaluator<?> evaluator) {
        try {
            Object result = evalUnsafe(evaluator);
            return ValueConversions.asBoolean(result);
        } catch (Exception e) {
            YesSteveModel.LOGGER.debug("Failed to evaluate molang expression.", e);
        }
        return false;
    }

    /**
     * 依次执行表达式，返回最后一个表达式的值，或第一个 return 语句的值。
     */
    default Object eval(ExpressionEvaluator<?> evaluator) {
        try {
            return evalUnsafe(evaluator);
        } catch (Exception e) {
            YesSteveModel.LOGGER.debug("Failed to evaluate molang expression.", e);
        }
        return null;
    }

    /**
     * 依次执行表达式，返回最后一个表达式的值，或第一个 return 语句的值，返回值的类型不确定，并且可能抛出异常。
     */
    Object evalUnsafe(ExpressionEvaluator<?> evaluator) throws Exception;
}
