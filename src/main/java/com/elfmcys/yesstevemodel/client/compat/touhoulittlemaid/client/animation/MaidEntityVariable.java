package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.LambdaVariable;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

public class MaidEntityVariable extends LambdaVariable<EntityMaid> {
    public MaidEntityVariable(IValueEvaluator<?, IContext<EntityMaid>> evaluator) {
        super(evaluator);
    }

    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof EntityMaid;
    }
}
