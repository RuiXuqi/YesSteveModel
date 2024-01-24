package com.elfmcys.yesstevemodel.geckolib3.core.manager;

import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatableModel;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;


public class SingletonAnimationFactory implements AnimationFactory {
    private final Int2ObjectOpenHashMap<AnimationData> animationDataMap = new Int2ObjectOpenHashMap<>();
    private final IAnimatable animatable;

    public SingletonAnimationFactory(IAnimatable animatable) {
        this.animatable = animatable;
    }

    @Override
    public AnimationData getOrCreateAnimationData(int uniqueID, IAnimatableModel<?> model) {
        if (!this.animationDataMap.containsKey(uniqueID)) {
            AnimationData data = new AnimationData();
            this.animatable.registerControllers(data, model);
            this.animationDataMap.put(uniqueID, data);
        }
        return this.animationDataMap.get(uniqueID);
    }
}