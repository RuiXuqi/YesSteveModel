package com.elfmcys.ysm.client.compat.slashblade;

import com.elfmcys.ysm.client.animation.molang.CtrlBinding;

public class SlashBladeBinding {
    static void addInnerBinding(CtrlBinding binding) {
        binding.livingEntityVar("slashblade_animation", SlashBladeAnimation::getAnimationName);
    }
}
