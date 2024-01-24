package com.elfmcys.yesstevemodel.geckolib3.core.manager;

import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatableModel;

public class InstancedAnimationFactory implements AnimationFactory {
    private final IAnimatable animatable;
    private AnimationData animationData;

    public InstancedAnimationFactory(IAnimatable animatable) {
        this.animatable = animatable;
    }

    @Override
    public AnimationData getOrCreateAnimationData(int uniqueID, IAnimatableModel<?> model) {
        if (this.animationData == null) {
            this.animationData = new AnimationData();
            this.animatable.registerControllers(this.animationData, model);
        }
        return this.animationData;
    }
}