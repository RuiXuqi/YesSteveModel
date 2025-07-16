package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;
import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playLoopAnimation;

public class CapPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    @Override
    public PlayState test(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        CustomPlayerEntity animatable = event.getAnimatableEntity();
        if (animatable instanceof IPreviewEntity guiEntity) {
            if (guiEntity.getPreviewInfo().hasPreview()) {
                return playLoopAnimation(event, guiEntity.getPreviewInfo().getPreview());
            }
            return PlayState.STOP;
        }

        if (animatable.isPlayingExtraAnimation()) {
            if (animatable.isExtraAnimationDirty()) {
                animatable.clearExtraAnimationDirty();
                event.getCodedController().indicateReload();
            }
            return playAnimation(event, animatable.getExtraAnimationName());
        }
        return PlayState.STOP;
    }
}
