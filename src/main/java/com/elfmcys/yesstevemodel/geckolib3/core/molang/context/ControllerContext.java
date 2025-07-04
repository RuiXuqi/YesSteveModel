package com.elfmcys.yesstevemodel.geckolib3.core.molang.context;

public class ControllerContext {
    private boolean allAnimationsFinished;
    private boolean anyAnimationFinished;

    public void setAllAnimationsFinished(boolean allAnimationsFinished) {
        this.allAnimationsFinished = allAnimationsFinished;
    }

    public void setAnyAnimationFinished(boolean anyAnimationFinished) {
        this.anyAnimationFinished = anyAnimationFinished;
    }

    public boolean isAllAnimationsFinished() {
        return allAnimationsFinished;
    }

    public boolean isAnyAnimationFinished() {
        return anyAnimationFinished;
    }
}
