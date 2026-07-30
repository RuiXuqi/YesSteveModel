package com.elfmcys.ysm.geckolib3.core.molang.function.entity;

import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.function.ContextFunction;
import net.minecraft.world.entity.projectile.Arrow;

public abstract class ArrowEntityFunction extends ContextFunction<Arrow> {
    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Arrow;
    }
}
