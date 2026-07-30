package com.elfmcys.ysm.client.animation.molang.variable;

import com.elfmcys.ysm.client.entity.CustomPlayerEntity;
import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.variable.IValueEvaluator;
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
