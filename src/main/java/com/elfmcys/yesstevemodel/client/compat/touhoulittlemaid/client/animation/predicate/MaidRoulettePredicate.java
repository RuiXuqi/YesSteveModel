package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.CustomYsmMaidEntity;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class MaidRoulettePredicate implements IAnimationPredicate<CustomYsmMaidEntity> {
    @Override
    public PlayState test(AnimationEvent<CustomYsmMaidEntity> event, ExpressionEvaluator<?> evaluator) {
        CustomYsmMaidEntity animatable = event.getAnimatableEntity();
        if (animatable instanceof IPreviewEntity guiEntity) {
            if (guiEntity.getPreviewInfo().hasPreview()) {
                return IAnimationPredicate.playLoopAnimation(event, guiEntity.getPreviewInfo().getPreview());
            }
            return PlayState.STOP;
        }

        if (animatable.isRouletteAnimPlaying()) {
            if (animatable.isRouletteAnimDirty()) {
                animatable.clearRouletteAnimDirty();
                event.getCodedController().indicateReload();
            }
            return playAnimation(event, animatable.getRouletteAnim());
        }
        return PlayState.STOP;
    }
}
