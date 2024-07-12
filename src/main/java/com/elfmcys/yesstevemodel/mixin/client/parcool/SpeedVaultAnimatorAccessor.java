package com.elfmcys.yesstevemodel.mixin.client.parcool;

import com.alrex.parcool.client.animation.impl.SpeedVaultAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpeedVaultAnimator.class)
public interface SpeedVaultAnimatorAccessor {
    @Accessor("type")
    SpeedVaultAnimator.Type getType();
}
