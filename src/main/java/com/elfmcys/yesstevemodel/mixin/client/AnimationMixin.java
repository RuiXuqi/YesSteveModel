package com.elfmcys.yesstevemodel.mixin.client;

import com.alrex.parcool.client.animation.Animator;
import com.alrex.parcool.common.capability.Animation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Animation.class)
public interface AnimationMixin {
    @Accessor("animator")
    Animator getAnimator();
}
