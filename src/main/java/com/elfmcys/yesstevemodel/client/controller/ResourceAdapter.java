package com.elfmcys.yesstevemodel.client.controller;

import com.elfmcys.yesstevemodel.client.animation.condition.ConditionArmor;
import com.elfmcys.yesstevemodel.client.model.CommonAsset;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;

public interface ResourceAdapter<T> {
    Object2ReferenceMap<String, AnimationControllerData> getControllers(T model, CommonAsset assets);
    Object2ReferenceMap<String, Animation> getAnimations(T model, CommonAsset assets);
    ConditionArmor getArmorCondition(T model, CommonAsset assets);
}
