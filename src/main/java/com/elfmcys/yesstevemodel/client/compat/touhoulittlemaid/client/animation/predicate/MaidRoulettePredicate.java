package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.CustomYsmMaidEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class MaidRoulettePredicate implements IAnimationPredicate<CustomYsmMaidEntity> {
    @Override
    public PlayState test(AnimationEvent<CustomYsmMaidEntity> event, ExpressionEvaluator<?> evaluator) {
        CustomYsmMaidEntity animatable = event.getAnimatableEntity();

        return animatable.getEntity().getCapability(YsmMaidCapabilityProvider.CAP).map(cap -> {
            if (cap.isRouletteAnimPlaying()) {
                if (cap.isRouletteAnimDirty()) {
                    cap.clearRouletteAnimDirty();
                    event.getCodedController().forceReload();
                }
                return playAnimation(event, cap.getRouletteAnim());
            }
            return PlayState.STOP;
        }).orElse(PlayState.STOP);
    }
}
