package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;
import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playLoopAnimation;

public class CapPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    @Override
    public PlayState test(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        CustomPlayerEntity animatable = event.getAnimatableEntity();
        if (animatable.hasPreviewAnimation()) {
            return playLoopAnimation(event, animatable.getPreviewAnimation());
        }

        return animatable.getEntity().getCapability(PlayerAnimatableCapabilityProvider.CAP).map(cap -> {
            if (cap.isPlayingAnimation()) {
                if (cap.isAnimationDirty()) {
                    cap.clearAnimationDirty();
                    event.getCodedController().forceReload();
                }
                return playAnimation(event, cap.getAnimationName());
            }
            return PlayState.STOP;
        }).orElse(PlayState.STOP);
    }
}
