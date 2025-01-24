package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
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

        return animatable.getEntity().getCapability(PlayerGeoCapabilityProvider.CAP).map(cap -> {
            if (cap.isPlayingAnimation()) {
                if (cap.isAnimationDirty()) {
                    cap.clearAnimationDirty();
                    event.getCodedController().markNeedsReload();
                }
                return playAnimation(event, cap.getAnimationName());
            }
            // 在轮盘动画没有播放时，需要关闭轮盘的音频播放
            event.getCodedController().stopSoundKeyFrames();
            return PlayState.STOP;
        }).orElse(PlayState.STOP);
    }
}
