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
    private final List<IAnimationController> animationControllers = new ReferenceArrayList<>(41);
    public double tick;
    public boolean isFirstTick = true;
    public double startTick = -1;
    public boolean shouldPlayWhilePaused = false;
    private double resetTickLength = 1;

    public AnimationData() {
    }

    public void addAnimationController(IAnimationController value) {
        animationControllers.add(value);
    }

    public double getResetSpeed() {
        return resetTickLength;
    }

    /**
     * 这是任何没有动画的骨骼恢复到其初始位置所需的时间
     *
     * @param resetTickLength 重置时所需的 tick。不能为负数
     */
    public void setResetSpeedInTicks(double resetTickLength) {
        this.resetTickLength = resetTickLength < 0 ? 0 : resetTickLength;
    }

    public List<IAnimationController> getAnimationControllers() {
        return animationControllers;
    }
}
