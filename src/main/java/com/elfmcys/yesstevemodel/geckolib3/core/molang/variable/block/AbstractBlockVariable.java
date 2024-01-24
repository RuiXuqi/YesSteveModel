package com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.block;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.LambdaVariable;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class AbstractBlockVariable extends LambdaVariable<BlockBehaviour> {
    public AbstractBlockVariable(IValueEvaluator<?, IContext<BlockBehaviour>> evaluator) {
        super(evaluator);
    }

    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof BlockBehaviour;
    }
}
