/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core.keyframe;

import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationMolangContext;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public abstract class AnimationPoint {
    /**
     * 当前关键帧播放进度
     */
    public final float currentTick;
    /**
     * 当前关键帧总长度
     */
    public final float totalTick;
    /**
     * 与动画相关的 molang 上下文
     */
    private final AnimationContext context;

    public AnimationPoint(float currentTick, float totalTick, AnimationContext context) {
        this.currentTick = currentTick;
        this.totalTick = totalTick;
        this.context = context;
    }

    protected float getPercentCompleted() {
        return totalTick == 0 ? 1 : (currentTick / totalTick);
    }

    protected void setupAnimationContext(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
        evaluator.entity().setAnimationContext(context);
    }

    public abstract Vector3f getLerpPoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator);
}
