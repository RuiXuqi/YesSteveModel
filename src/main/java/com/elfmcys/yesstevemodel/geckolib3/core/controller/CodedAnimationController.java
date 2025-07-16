package com.elfmcys.yesstevemodel.geckolib3.core.controller;

import com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimationState;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.LoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.transition.LinearBlendTransition;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.*;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.point.EndingTransitionPoint;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.core.util.MathUtil;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class CodedAnimationController<T extends AnimatableEntity<?>> implements IAnimationController<T> {
    private final String name;
    private final IAnimationPredicate<T> animationPredicate;
    private final AnimationPlayer animationPlayer;
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
        this.blendRotation = blendRotation;
    }

    @Override
    public void process(AnimationEvent<T> event, ExpressionEvaluator<MolangContext<?>> evaluator, boolean allowEmitting) {
        event.setCodedAnimationController(this);
        PlayState playState = this.animationPredicate.test(event, evaluator);

        if (playState == PlayState.CONTINUE) {
            this.animationPlayer.process(event.renderTicks, evaluator, allowEmitting);
        } else if (playState == PlayState.STOP) {
            var state = this.animationPlayer.getState();
            if (state == AnimationState.BEGINNING_TRANSITION || state == AnimationState.RUNNING) {
                this.animationPlayer.stop(event.renderTicks);
                this.animationPlayer.indicateReload();
            }
            if (state == AnimationState.ENDING_TRANSITION) {
                this.animationPlayer.process(event.renderTicks, evaluator, allowEmitting);
            }
        } else {
            this.animationPlayer.resetBoneAnimationQueues();
        }
    }

    @Override
    public void updateModelBones(List<BoneTopLevelSnapshot> modelRendererList) {
        this.animationPlayer.updateModel(modelRendererList);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    public String getState() {
        // 硬编码控制器没有状态，返回自己名称+正在播放的动画
        if (animationPlayer.getState() == AnimationState.IDLE) {
            return "Coded";
        } else {
            return "Coded -> " + animationPlayer.getCurrentAnim().name;
        }
    }

    public void setAnimation(String animationName) {
        this.animationPlayer.setAnimation(animationName, null);
    }

    public void setAnimation(String animationName, @Nullable LoopType loopType) {
        this.animationPlayer.setAnimation(animationName, loopType);
    }

    public void setBeginningTransitionLength(float sec) {
        if (this.animationPlayer.getBeginningTransitionLength() != sec) {
            this.animationPlayer.setBeginningTransition(new LinearBlendTransition(sec));
        }
    }

    @Override
    public void visitBoneAnimationQueues(Consumer<IBoneAnimationQueue> visitor) {
        for (var queue : this.animationPlayer.getActiveBoneAnimQueues()) {
            visitor.accept(new SingleBoneAnimationQueue(queue));
        }
    }

    public void reset() {
        this.animationPlayer.reset();
    }

    public void indicateReload() {
        this.animationPlayer.indicateReload();
    }

    public void stop(float renderTicks) {
        this.animationPlayer.stop(renderTicks);
    }

    public boolean isAnimFinished() {
        return this.animationPlayer.currentAnimFinished();
    }

    public void stopPlayingSounds() {
        this.animationPlayer.stopPlayingSounds();
    }

    @Override
    @Deprecated
    public boolean blendRotation() {
        // TODO: 仅临时缓解，未完全修复过渡动画混合问题。
        return blendRotation && animationPlayer.getState() == AnimationState.RUNNING;
    }

    private record SingleBoneAnimationQueue(BoneAnimationQueue queue) implements IBoneAnimationQueue {
        @Override
        public BoneTopLevelSnapshot getSnapshot() {
            return queue.topLevelSnapshot;
        }

        @Override
        public Optional<AnimationVec3> pollRotationPoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
            var point = this.queue.rotation;

            if (point == null) {
                return Optional.empty();
            }

            var pointValue = new AnimationVec3(point.getLerpPoint(evaluator));
            if (point instanceof EndingTransitionPoint endingPoint) {
                pointValue.setEndingTransitionPercentProgressIfLess(endingPoint.getPercentCompleted());
            } else {
                pointValue.setEndingTransitionPercentProgressIfLess(0);
            }

            var weight = queue.getBlendWeight();
            if (weight != 1) {
                pointValue.mul(weight);
            }
            return Optional.of(pointValue);
        }

        @Override
        public Optional<AnimationVec3> pollPositionPoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
            var point = this.queue.position;

            if (point == null) {
                return Optional.empty();
            }

            var pointValue = new AnimationVec3(point.getLerpPoint(evaluator));
            if (point instanceof EndingTransitionPoint endingPoint) {
                pointValue.setEndingTransitionPercentProgressIfLess(endingPoint.getPercentCompleted());
            } else {
                pointValue.setEndingTransitionPercentProgressIfLess(0);
            }

            var weight = queue.getBlendWeight();
            if (weight != 1) {
                pointValue.mul(weight);
            }
            return Optional.of(pointValue);
        }

        @Override
        public Optional<AnimationVec3> pollScalePoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
            var point = this.queue.scale;

            if (point == null) {
                return Optional.empty();
            }

            var pointValue = new AnimationVec3(point.getLerpPoint(evaluator));
            if (point instanceof EndingTransitionPoint endingPoint) {
                pointValue.setEndingTransitionPercentProgressIfLess(endingPoint.getPercentCompleted());
            } else {
                pointValue.setEndingTransitionPercentProgressIfLess(0);
            }

            var weight = queue.getBlendWeight();
            if (weight != 1) {
                MathUtil.computeWeightedScale(pointValue, weight, pointValue);
            }
            return Optional.of(pointValue);
        }
    }
}
