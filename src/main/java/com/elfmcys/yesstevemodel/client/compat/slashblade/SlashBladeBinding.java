package com.elfmcys.yesstevemodel.client.compat.slashblade;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;

public class SlashBladeBinding {
    static void addInnerBinding(CtrlBinding binding) {
        binding.livingEntityVar("slashblade_animation", SlashBladeAnimation::getAnimationName);
    }
}
