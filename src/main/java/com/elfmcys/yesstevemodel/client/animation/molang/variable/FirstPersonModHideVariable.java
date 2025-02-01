package com.elfmcys.yesstevemodel.client.animation.molang.variable;

import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import com.elfmcys.yesstevemodel.util.PersonView;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.AbstractClientPlayer;

public class FirstPersonModHideVariable implements IValueEvaluator<Boolean, IContext<AbstractClientPlayer>> {
    @Override
    public Boolean eval(IContext<AbstractClientPlayer> ctx) {
        if (FirstPersonCompat.isInstalled() && PersonView.getPersonView(ctx) == CameraType.FIRST_PERSON.ordinal()) {
            return FirstPersonCompat.shouldHideHead();
        } else {
            return false;
        }
    }
}
