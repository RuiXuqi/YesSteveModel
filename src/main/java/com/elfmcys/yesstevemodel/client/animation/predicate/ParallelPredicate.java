package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

public class ParallelPredicate<T extends AnimatableEntity<?>> implements IAnimationPredicate<T> {
    private final String animationName;

    public ParallelPredicate(String animationName) {
        this.animationName = animationName;
    }

    @Override
    public PlayState test(AnimationEvent<T> event, ExpressionEvaluator<?> evaluator) {
        return IAnimationPredicate.playLoopAnimation(event, animationName);
    }
}
