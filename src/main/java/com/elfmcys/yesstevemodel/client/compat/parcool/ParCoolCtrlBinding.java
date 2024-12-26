package com.elfmcys.yesstevemodel.client.compat.parcool;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import net.minecraft.client.player.AbstractClientPlayer;
import org.apache.commons.lang3.StringUtils;

public class ParCoolCtrlBinding {
    static void addInnerBinding(CtrlBinding binding) {
        binding.playerVar("parcool_state", ParCoolCtrlBinding::getState);
    }

    private static String getState(IContext<AbstractClientPlayer> context) {
        String animation = ParCoolAnimationManger.getAnimation(context.entity());
        if (animation != null) {
            return animation.substring("parcool:".length());
        }
        return StringUtils.EMPTY;
    }
}
