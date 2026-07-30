package com.elfmcys.ysm.geckolib3.core.molang.function.entity;

import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.function.ContextFunction;
import net.minecraft.world.entity.Mob;

public abstract class MobEntityFunction extends ContextFunction<Mob> {
    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Mob;
    }
}
