package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.apache.commons.lang3.StringUtils;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playLoopAnimation;

public class HoverPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    @Override
    public PlayState test(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        String hoverAnimation = ((IPreviewEntity) event.getAnimatableEntity()).getPreviewInfo().getHover();
        if (StringUtils.isNoneBlank(hoverAnimation)) {
            return playLoopAnimation(event, hoverAnimation);
        }
        return PlayState.STOP;
    }
}
