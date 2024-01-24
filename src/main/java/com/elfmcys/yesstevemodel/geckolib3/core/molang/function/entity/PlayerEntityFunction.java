package com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.ContextFunction;
import net.minecraft.client.player.AbstractClientPlayer;

public abstract class PlayerEntityFunction extends ContextFunction<AbstractClientPlayer> {
    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof AbstractClientPlayer;
    }
}
