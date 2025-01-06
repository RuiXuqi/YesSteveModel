package com.elfmcys.yesstevemodel.client.animation.molang.variable;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import net.minecraft.client.player.AbstractClientPlayer;

public class TextureNameVariable implements IValueEvaluator<String, IContext<AbstractClientPlayer>> {
    @Override
    public String eval(IContext<AbstractClientPlayer> ctx) {
        if (ctx.animatableEntity() instanceof CustomPlayerEntity instance) {
            return instance.getTextureName();
        } else {
            return null;
        }
    }
}
