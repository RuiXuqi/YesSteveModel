package com.elfmcys.ysm.geckolib3.core.molang.variable.entity;

import com.elfmcys.ysm.geckolib3.core.molang.variable.IValueEvaluator;
import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.variable.LambdaVariable;
import net.minecraft.world.entity.player.Player;

public class PlayerVariable extends LambdaVariable<Player> {
    public PlayerVariable(IValueEvaluator<?, IContext<Player>> evaluator) {
        super(evaluator);
    }

    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Player;
    }
}
