package com.elfmcys.yesstevemodel.client.animation.molang.variable;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import net.minecraft.world.entity.player.Player;

public class TextureNameVariable implements IValueEvaluator<String, IContext<Player>> {
    @Override
    public String eval(IContext<Player> ctx) {
        if (ctx.animatableEntity() instanceof CustomPlayerEntity animatableEntity) {
            return animatableEntity.getTextureName();
        } else {
            return null;
        }
    }
}
