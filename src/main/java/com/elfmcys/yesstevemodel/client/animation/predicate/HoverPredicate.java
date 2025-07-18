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
        var previewEntity = (IPreviewEntity) event.getAnimatableEntity();
        String hoverAnimation = previewEntity.getPreviewInfo().getHover();
        if (StringUtils.isNoneBlank(hoverAnimation)) {
            previewEntity.setAllowEmitting(true);
            return playLoopAnimation(event, hoverAnimation);
        }
        previewEntity.setAllowEmitting(false);
        event.getCodedController().stopPlayingSounds();
        return PlayState.STOP;
    }
}
