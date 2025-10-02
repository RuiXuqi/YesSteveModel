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

    /**
     * 自 2.4.2-snapshot-27 版本起，所有的手部动画全部交由动画文件本身决定播放类型
     * 部分旧版加密模型可能会动画错误，特此保留此方法以兼容旧版加密模型
     */
    @NotNull
    static <P extends AnimatableEntity<?>> PlayState playCompatAnimation(AnimationEvent<P> event, String animationName, LoopType loopType, int formatVer) {
        // 未加密的模型是 0，旧版（1.1.x 版本）加密是 -1，2.4.2-snapshot-21 版本序号是 18，snapshot-32 是 19
        // 未加密模型和 19 序号（包含）之后的都直接让动画文件决定播放类型
        if (formatVer == 0 || formatVer >= 19) {
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
