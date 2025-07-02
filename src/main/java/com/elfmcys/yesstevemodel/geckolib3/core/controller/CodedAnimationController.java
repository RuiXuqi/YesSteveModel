package com.elfmcys.yesstevemodel.geckolib3.core.controller;

import com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimationState;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.AnimationBuilder;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.AnimationPoint;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.BoneAnimationQueue;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationMolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class CodedAnimationController<T extends AnimatableEntity<?>> implements IAnimationController<T> {
    private final String name;
    private final IAnimationPredicate<T> animationPredicate;
    private final AnimationPlayer animationPlayer;
    private final ReferenceArrayList<SingleBoneAnimationQueue> boneAnimationQueues;
    private final boolean blendRotation;

    /**
     * 实例化硬编码动画控制器，每个控制器同一时间只能播放一个动画 <br>
     * 你可以为一个实体附加多个动画控制器 <br>
     * 比如一个控制器控制实体大小，另一个控制移动，攻击等等
     *
     * @param animatableEntity      实体
     * @param name                  动画控制器名称
     * @param transitionLengthTicks 动画过渡时间（tick）
     */
    public CodedAnimationController(T animatableEntity, String name, float transitionLengthTicks,
                                    IAnimationPredicate<T> animationPredicate) {
        this(animatableEntity, name, transitionLengthTicks, animationPredicate, false);
    }

    @Deprecated
    public CodedAnimationController(T animatableEntity, String name, float transitionLengthTicks,
                                    IAnimationPredicate<T> animationPredicate, boolean blendRotation) {
        this.name = name;
        this.animationPredicate = animationPredicate;
        this.animationPlayer = new AnimationPlayer(animatableEntity, transitionLengthTicks);
        this.boneAnimationQueues = new ReferenceArrayList<>();
        this.blendRotation = blendRotation;
    }

    @Override
    public void process(final float tick, AnimationEvent<T> event, ExpressionEvaluator<AnimationMolangContext<?>> evaluator, boolean scheduledUpdate) {
        event.setCodedAnimationController(this);
        PlayState playState = this.animationPredicate.test(event, evaluator);
        if (playState == PlayState.CONTINUE) {
            this.animationPlayer.process(tick, evaluator, scheduledUpdate, false);
        } else {
            this.animationPlayer.stop();
        }
    }

    @Override
    public void updateRenderer(List<BoneTopLevelSnapshot> modelRendererList) {
        this.animationPlayer.updateRenderer(modelRendererList);
        this.boneAnimationQueues.clear();
        for (var queue : this.animationPlayer.getBoneAnimationQueues().values()) {
            this.boneAnimationQueues.add(new SingleBoneAnimationQueue(queue));
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getState() {
        // 硬编码控制器没有状态，返回自己名称
        return "Coded Controller";
    }

    public void setAnimation(AnimationBuilder builder) {
        this.animationPlayer.setAnimation(builder);
    }

    @Override
    public void visitBoneAnimationQueues(Consumer<IBoneAnimationQueue> visitor) {
        for (var queue : this.boneAnimationQueues) {
            if (queue.isActive()) {
                visitor.accept(queue);
            }
        }
    }

    public void resetTick() {
        this.animationPlayer.shouldResetTick = true;
        this.animationPlayer.adjustTick(0);
    }

    public void markNeedsReload() {
        this.animationPlayer.markNeedsReload();
    }

    public boolean isAnimFinished() {
        return this.animationPlayer.animIsFinished;
    }

    public void stopSoundKeyFrames() {
        this.animationPlayer.stopSoundKeyFrames();
    }

    @Override
    @Deprecated
    public boolean blendRotation() {
        return blendRotation && animationPlayer.animationState != AnimationState.TRANSITIONING;
    }

    private static class SingleBoneAnimationQueue implements IBoneAnimationQueue {
        private final BoneAnimationQueue queue;

        public SingleBoneAnimationQueue(BoneAnimationQueue queue) {
            this.queue = queue;
        }

        @Override
        public BoneTopLevelSnapshot getSnapshot() {
            return queue.topLevelSnapshot;
        }

        @Override
        public Optional<Vector3f> pollRotationPoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
            return pollAndBlend(this.queue.rotationQueue.poll(), evaluator);
        }

        @Override
        public Optional<Vector3f> pollPositionPoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
            return pollAndBlend(this.queue.positionQueue.poll(), evaluator);
        }

        @Override
        public Optional<Vector3f> pollScalePoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
            return pollAndBlend(this.queue.scaleQueue.poll(), evaluator);
        }

        private Optional<Vector3f> pollAndBlend(AnimationPoint point, ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
            if (point == null) {
                return Optional.empty();
            }
            var pointValue = point.getLerpPoint(evaluator);
            return Optional.of(pointValue.mul(queue.getBlendWeight()));
        }

        public boolean isActive() {
            return this.queue.isActive();
        }
    }
}
