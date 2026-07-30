package com.elfmcys.ysm.client.animation.predicate;

import com.elfmcys.ysm.geckolib3.core.PlayState;
import com.elfmcys.ysm.geckolib3.core.builder.LoopType;
import com.elfmcys.ysm.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.ysm.geckolib3.model.AnimatableEntity;
import com.elfmcys.ysm.info.ModelFormatVersion;
import com.elfmcys.ysm.molang.runtime.ExpressionEvaluator;
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

    /**
     * 自 2.4.2-snapshot-27 版本起，所有的手部动画全部交由动画文件本身决定播放类型
     * 部分旧版加密模型可能会动画错误，特此保留此方法以兼容旧版加密模型
     */
    @NotNull
    static <P extends AnimatableEntity<?>> PlayState playCompatAnimation(AnimationEvent<P> event, String animationName, LoopType loopType, int formatVer) {
        if (ModelFormatVersion.shouldIgnoreCodedLoopTypeForHandAnim(event, animationName, formatVer)) {
            event.getCodedController().setAnimation(animationName);
        } else {
            event.getCodedController().setAnimation(animationName, loopType);
        }
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
