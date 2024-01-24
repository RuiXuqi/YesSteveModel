package com.elfmcys.yesstevemodel.geckolib3.core.manager;

import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatableModel;

public interface AnimationFactory {
    AnimationData getOrCreateAnimationData(int uniqueID, IAnimatableModel<?> model);
}
