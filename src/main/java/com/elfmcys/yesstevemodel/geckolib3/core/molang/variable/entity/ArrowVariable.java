package com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.entity;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.LambdaVariable;
import net.minecraft.world.entity.projectile.Arrow;

public class ArrowVariable extends LambdaVariable<Arrow> {
    public ArrowVariable(IValueEvaluator<?, IContext<Arrow>> evaluator) {
        super(evaluator);
    }

    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Arrow;
    }
}
