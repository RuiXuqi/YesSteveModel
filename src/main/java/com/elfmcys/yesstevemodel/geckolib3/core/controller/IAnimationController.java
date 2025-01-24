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

    /**
     * 这个参数是为了处理一个 YSM 遗留问题
     * <p>
     * 曾经某个版本的 YSM 加入了并行动画的混合功能，但是仅混合动画旋转数值
     *
     * @return 如果是 CodedAnimationController，那么仅并行动画返回 true
     * <p>
     * 如果使用动画控制器，那么将永远返回 false
     */
    @Deprecated
    default boolean blendRotation() {
        return false;
    }
}
