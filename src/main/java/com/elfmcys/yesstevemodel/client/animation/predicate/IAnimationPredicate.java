package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.LoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface IAnimationPredicate<T extends AnimatableEntity<?>> {
    @NotNull
    static <T extends AnimatableEntity<?>> PlayState playAnimation(AnimationEvent<T> event, String animationName, LoopType loopType) {
        event.getCodedController().setAnimation(animationName, loopType);
        return PlayState.CONTINUE;
    }

    @NotNull
    static <P extends AnimatableEntity<?>> PlayState playAnimation(AnimationEvent<P> event, String animationName) {
        event.getCodedController().setAnimation(animationName);
        return PlayState.CONTINUE;
    }

    @NotNull
    static <T extends AnimatableEntity<?>> PlayState playLoopAnimation(AnimationEvent<T> event, String animationName) {
        return playAnimation(event, animationName, LoopType.LOOP);
    }

    /**
     * 每个 CodedAnimationController 每个关键帧都会运行一次 AnimationPredicate
     * 这个方法就是你判断动画是否能播放的地方
     */
    PlayState test(AnimationEvent<T> event, ExpressionEvaluator<?> evaluator);
}
