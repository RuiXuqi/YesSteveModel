package com.elfmcys.ysm.geckolib3.core.molang.builtin.query;

import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.function.ContextFunction;
import com.elfmcys.ysm.molang.runtime.ExecutionContext;

public class DebugOutput extends ContextFunction<Object> {
    @Override
    protected Object eval(ExecutionContext<IContext<Object>> context, ArgumentCollection arguments) {
        if(!context.entity().isDebugEnabled()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < arguments.size(); i++) {
            Object param = arguments.getValue(context, i);
            builder.append(param == null ? "null" : param);
        }
        context.entity().debugPrint(builder.toString());
        return null;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size > 0;
    }
}
