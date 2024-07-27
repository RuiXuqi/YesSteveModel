package com.elfmcys.yesstevemodel.geckolib3.core;

import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.AnimationProcessor;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IAnimatableModel<E extends IAnimatable<?>> {
    /**
     * 获取当前的 tick
     *
     * @return 当前的 tick
     */
    default double getCurrentTick() {
        return System.nanoTime() / 1000000.0 / 50.0;
    }

    /**
     * 设置自定义动画
     *
     * @param animatable     对象
     * @param ctx            molang 上下文
     * @param animationEvent 动画事件
     * @return               是否更新
     */
    default boolean setCustomAnimations(E animatable, AnimationContext<?> ctx, @NotNull AnimationEvent<E> animationEvent) {
        return false;
    }

    /**
     * 获取 Animation Processor
     *
     * @return AnimationProcessor
     */
    AnimationProcessor<E> getAnimationProcessor();

    /**
     * 获取动画
     *
     * @param name       动画名称
     * @param animatable 对象
     * @return 动画
     */
    Animation getAnimation(String name, E animatable);

    /**
     * 通过骨骼名获取 IBone
     *
     * @param boneName 骨骼名
     * @return IBone
     */
    @Nullable
    default IBone getBone(String boneName) {
        return getAnimationProcessor().getBone(boneName);
    }
}
