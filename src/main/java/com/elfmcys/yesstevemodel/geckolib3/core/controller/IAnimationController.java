package com.elfmcys.yesstevemodel.geckolib3.core.controller;

import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationMolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

import java.util.List;
import java.util.function.Consumer;

public interface IAnimationController<T extends AnimatableEntity<?>> {
    /**
     * 获取动画控制器名称
     */
    String getName();

    /**
     * 更新模型
     */
    void updateRenderer(List<BoneTopLevelSnapshot> modelRendererList);

    void process(final double tick, AnimationEvent<T> event, ExpressionEvaluator<AnimationMolangContext<?>> evaluator, boolean scheduledUpdate);

    void visitBoneAnimationQueues(Consumer<IBoneAnimationQueue> visitor);
}
