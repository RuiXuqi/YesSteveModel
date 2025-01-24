package com.elfmcys.yesstevemodel.geckolib3.core.controller;

import com.elfmcys.yesstevemodel.geckolib3.core.builder.AnimationBuilder;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationControllerState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.AnimationPoint;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.BoneAnimationQueue;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationMolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceLists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;


public class BedrockAnimationController<T extends AnimatableEntity<?>> implements IAnimationController<T> {
    private final T animatableEntity;
    private final String name;
    private final float initTransitionLengthTicks;

    @Nullable
    private List<BoneTopLevelSnapshot> modelRendererList;
    @Nullable
    private GeoAnimationController data;
    @Nullable
    private GeoAnimationControllerState state;

    private final ReferenceArrayList<AnimationPlayerHolder> animationPlayers = new ReferenceArrayList<>(8);
    private final ReferenceArrayList<BlendBoneAnimationQueue> blendAnimationQueues = new ReferenceArrayList<>(64);
    private int activeAnimationPlayerSize = 0;

    /**
     * 实例化基岩版动画控制器 <br>
     * 你可以为一个实体附加多个动画控制器 <br>
     * 比如一个控制器控制实体大小，另一个控制移动，攻击等等
     *
     * @param animatableEntity      实体
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
        if (this.data == null) {
            return;
        }

        // 更新状态
        if (this.state == null) {
            var initialState = this.data.states().get(this.data.initialState());
            if (initialState == null) {
                return;
            }
            updateState(initialState, evaluator);
        } else {
            for (var transition : state.transitions()) {
                if (!transition.getRight().evalAsBoolean(evaluator)) {
                    continue;
                }
                var newState = this.data.states().get(transition.getLeft());
                if (newState == null) {
                    return;
                }
                updateState(newState, evaluator);
            }
        }

        // 更新动画
        for (var i = 0; i < this.activeAnimationPlayerSize; i++) {
            var holder = this.animationPlayers.get(i);
            holder.animationPlayer().process(tick, evaluator, scheduledUpdate);
            holder.conditionHolder().evaluateApplyCondition(evaluator);
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
        clearRenderer();

        this.data = animationControllerData;
        for (var snapshot : modelRendererList) {
            this.blendAnimationQueues.add(new BlendBoneAnimationQueue(snapshot));
        }
        this.modelRendererList = modelRendererList;
    }

    public void clearRenderer() {
        this.modelRendererList = ReferenceLists.emptyList();
        this.data = null;
        this.state = null;
        this.activeAnimationPlayerSize = 0;
        this.blendAnimationQueues.clear();
        for (var holder : this.animationPlayers) {
            holder.markAsDirty();
        }
    }

    private void updateState(GeoAnimationControllerState state, ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
        assert this.modelRendererList != null;
        if (this.state != null) {
            for (var exp : this.state.onExit()) {
                exp.evalAsDouble(evaluator);
            }
        }
        for (var exp : state.onEntry()) {
            exp.evalAsDouble(evaluator);
        }
        this.state = state;

        // 扩容动画播放器列表
        for (var i = this.animationPlayers.size(); i < state.animations().size(); i++) {
            this.animationPlayers.add(new AnimationPlayerHolder(this.animatableEntity, this.initTransitionLengthTicks));
        }
        // 停用多余的动画播放器
        for (var i = state.animations().size(); i < this.activeAnimationPlayerSize; i++) {
            this.animationPlayers.get(i).animationPlayer().markNeedsReload();
        }
        // 初始化动画播放器
        this.activeAnimationPlayerSize = state.animations().size();
        for (var i = 0; i < state.animations().size(); i++) {
            var holder = this.animationPlayers.get(i);
            var animPair = state.animations().get(i);

            if (holder.isDirty()) {
                holder.animationPlayer().updateRenderer(this.modelRendererList);
                for (var queue : this.blendAnimationQueues) {
                    queue.addUnderlyingQueue(holder.conditionHolder(), holder.animationPlayer().getBoneAnimationQueues().get(queue.boneName()));
                }
                holder.clearDirty();
            }

            holder.conditionHolder().setApplyCondition(animPair.getRight());
            holder.animationPlayer().transition = state.blendTransition().startNew();
            holder.animationPlayer().setAnimation(new AnimationBuilder().addAnimation(animPair.getLeft()));
        }
    }

    @Override
    public void visitBoneAnimationQueues(Consumer<IBoneAnimationQueue> visitor) {
        for (var queue : this.blendAnimationQueues) {
            if (queue.isActive()) {
                visitor.accept(queue);
            }
        }
    }

    private static class AnimationPlayerHolder {
        private final ConditionHolder conditionHolder;
        private final AnimationPlayer animationPlayer;
        private boolean dirty;

        private AnimationPlayerHolder(AnimatableEntity<?> animatableEntity, float transitionLengthTicks) {
            conditionHolder = new ConditionHolder();
            animationPlayer = new AnimationPlayer(animatableEntity, transitionLengthTicks);
            dirty = true;
        }

        public ConditionHolder conditionHolder() {
            return conditionHolder;
        }

        public AnimationPlayer animationPlayer() {
            return animationPlayer;
        }

        public boolean isDirty() {
            return dirty;
        }

        public void markAsDirty() {
            dirty = true;
        }

        public void clearDirty() {
            dirty = false;
        }
    }

    private static class ConditionHolder {
        @Nullable
        private IValue applyCondition;
        private boolean result;

        public ConditionHolder() {
            result = true;
        }

        public void setApplyCondition(@Nullable IValue applyCondition) {
            this.applyCondition = applyCondition;
            if (applyCondition == null) {
                result = true;
            }
        }

        public void evaluateApplyCondition(ExpressionEvaluator<?> evaluator) {
            if (applyCondition != null) {
                result = applyCondition.evalAsBoolean(evaluator);
            }
        }

        public boolean shouldApply() {
            return result;
        }
    }

    private static class BlendBoneAnimationQueue implements IBoneAnimationQueue {
        private final BoneTopLevelSnapshot snapshot;
        private final ReferenceArrayList<Pair<ConditionHolder, BoneAnimationQueue>> underlyingQueues;

        private final Vector3f initRotation;
        private final Vector3f initPosition;
        private final Vector3f initScale;

        public BlendBoneAnimationQueue(BoneTopLevelSnapshot snapshot) {
            this.snapshot = snapshot;
            this.underlyingQueues = new ReferenceArrayList<>(4);

            var initState = snapshot.bone.getInitialSnapshot();
            this.initRotation = new Vector3f(initState.rotationValueX, initState.rotationValueY, initState.rotationValueZ);
            this.initPosition = new Vector3f(initState.positionOffsetX, initState.positionOffsetY, initState.positionOffsetZ);
            this.initScale = new Vector3f(initState.scaleValueX, initState.scaleValueY, initState.scaleValueZ);
        }

        public String boneName() {
            return snapshot.name;
        }

        public void addUnderlyingQueue(ConditionHolder conditionHolder, BoneAnimationQueue queue) {
            this.underlyingQueues.add(Pair.of(conditionHolder, queue));
        }

        public boolean isActive() {
            if (this.underlyingQueues.isEmpty()) {
                return false;
            }
            for (var pair : this.underlyingQueues) {
                if (pair.left().shouldApply() && pair.right().isActive()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public BoneTopLevelSnapshot getSnapshot() {
            return snapshot;
        }

        @Override
        public Optional<Vector3f> pollRotationPoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
            return pollAndBlend(initRotation, queue -> queue.rotationQueue.poll(), evaluator);
        }

        @Override
        public Optional<Vector3f> pollPositionPoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
            return pollAndBlend(initPosition, queue -> queue.positionQueue.poll(), evaluator);
        }

        @Override
        public Optional<Vector3f> pollScalePoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
            return pollAndBlend(initScale, queue -> queue.scaleQueue.poll(), evaluator);
        }

        private Optional<Vector3f> pollAndBlend(Vector3f init, Function<BoneAnimationQueue, @Nullable AnimationPoint> pointGetter, ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
            var target = new Vector3f(init);
            var active = false;

            for (var pair : this.underlyingQueues) {
                var queue = pair.right();
                if (!queue.isActive()) {
                    continue;
                }
                var point = pointGetter.apply(queue);
                if (point == null) {
                    continue;
                }
                if (!pair.left().shouldApply()) {
                    continue;
                }
                active = true;
                var pointValue = point.getLerpPoint(evaluator);
                var delta = pointValue.sub(init);
                target.fma(pair.right().getBlendWeight(), delta);
            }

            if (active) {
                return Optional.of(target);
            } else {
                return Optional.empty();
            }
        }
    }
}
