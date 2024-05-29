package com.elfmcys.yesstevemodel.client.animation.molang.variable;

import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;

public class FirstPersonModHideVariable implements IValueEvaluator<Boolean, IContext<AbstractClientPlayer>> {
    @Override
    public Boolean eval(IContext<AbstractClientPlayer> ctx) {
        if (ctx.entity() instanceof LocalPlayer && FirstPersonCompat.isInstalled()) {
            return FirstPersonCompat.shouldHideHead();
        } else {
            return false;
        }
    }
}
