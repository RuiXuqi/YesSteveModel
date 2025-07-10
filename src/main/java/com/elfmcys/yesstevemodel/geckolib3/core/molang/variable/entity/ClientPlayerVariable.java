package com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.entity;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.LambdaVariable;
import net.minecraft.client.player.AbstractClientPlayer;

public class ClientPlayerVariable extends LambdaVariable<AbstractClientPlayer> {
    public ClientPlayerVariable(IValueEvaluator<?, IContext<AbstractClientPlayer>> evaluator) {
        super(evaluator);
    }

    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof AbstractClientPlayer;
    }
}
