package com.elfmcys.ysm.client.animation.molang.variable;

import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.variable.IValueEvaluator;
import net.minecraft.world.entity.player.Player;

public class FirstPersonModHideVariable implements IValueEvaluator<Boolean, IContext<Player>> {
    @Override
    public Boolean eval(IContext<Player> ctx) {
        return ctx.animationEvent().getRenderContext().firstPersonMod();
    }
}
