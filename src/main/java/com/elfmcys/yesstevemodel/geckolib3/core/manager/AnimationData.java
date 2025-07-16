/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core.manager;

import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.List;

@SuppressWarnings("rawtypes")
public class AnimationData {
    public static final float DEFAULT_ENDING_TRANSITION_LENGTH = 3;

    private final List<IAnimationController> animationControllers = new ReferenceArrayList<>(41);
    public float lastTick;
    public float startTick = -1;
    // 全局尾过渡动画的长度，一定不能小于 1
    private float resetTickLength = DEFAULT_ENDING_TRANSITION_LENGTH;

    public AnimationData() {
    }

    public void addAnimationController(IAnimationController value) {
        animationControllers.add(value);
    }

    public float getResetSpeed() {
        return resetTickLength;
    }

    /**
     * 这是任何没有动画的骨骼恢复到其初始位置所需的时间
     *
     * @param resetTickLength 重置时所需的 tick。不能为负数
     */
    public void setResetSpeedInTicks(float resetTickLength) {
        this.resetTickLength = resetTickLength < 0 ? 0 : resetTickLength;
    }

    public List<IAnimationController> getAnimationControllers() {
        return animationControllers;
    }
}
