package com.elfmcys.yesstevemodel.geckolib3.core.controller;

import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationControllerState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationMolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// FIXME: 完成混合动画支持
public class BedrockAnimationController<T extends AnimatableEntity<?>> implements IAnimationController<T> {
    private final T animatableEntity;
    private final String name;
    private final float initTransitionLengthTicks;

    @Nullable
    private GeoAnimationController data;
    @Nullable
    private GeoAnimationControllerState state;

    private final ReferenceArrayList<Pair<@Nullable IValue, AnimationPlayer>> animationPlayers = new ReferenceArrayList<>(8);
    private final Object2ReferenceOpenHashMap<String, BlendBoneAnimationQueue> animationQueues = new Object2ReferenceOpenHashMap<>(64);

    /**
     * 实例化基岩版动画控制器 <br>
     * 你可以为一个实体附加多个动画控制器 <br>
     * 比如一个控制器控制实体大小，另一个控制移动，攻击等等
     *
     * @param animatableEntity            实体
     * @param name                  动画控制器名称
     * @param transitionLengthTicks 动画过渡时间（tick）
     */
    public BedrockAnimationController(T animatableEntity, String name, float transitionLengthTicks) {
        this.animatableEntity = animatableEntity;
        this.name = name;
        this.initTransitionLengthTicks = transitionLengthTicks;
    }

    @Override
    public void process(final double tick, AnimationEvent<T> event, ExpressionEvaluator<AnimationMolangContext<?>> evaluator, boolean scheduledUpdate) {

        // process


        for (var queue : this.animationQueues.values()) {
            queue.process();
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void updateRenderer(List<BoneTopLevelSnapshot> modelRendererList) {
        var data = this.animatableEntity.getAnimationControllerData(this.name);
        if (data != null) {
            this.updateRenderer(modelRendererList, data);
        } else {
            this.clearRenderer();
        }
    }

    public void updateRenderer(List<BoneTopLevelSnapshot> modelRendererList, @NotNull GeoAnimationController animationControllerData) {

        this.animationQueues.clear();
    }

    public void clearRenderer() {
        this.animationQueues.clear();
    }

    @Override
    public void visitBoneAnimationQueues(Consumer<IBoneAnimationQueue> visitor) {
        for (var queue : this.animationQueues.values()) {
            if (queue.isActive()) {
                visitor.accept(queue);
            }
        }
    }

    private static class BlendBoneAnimationQueue implements IBoneAnimationQueue {
        @Override
        public BoneTopLevelSnapshot getSnapshot() {
            return null;
        }

        @Override
        public Optional<Vector3f> pollRotationPoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
            return Optional.empty();
        }

        @Override
        public Optional<Vector3f> pollPositionPoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
            return Optional.empty();
        }

        @Override
        public Optional<Vector3f> pollScalePoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
            return Optional.empty();
        }

        public void process() {

        }

        public boolean isActive() {
            return false;
        }
    }
}
