package com.elfmcys.yesstevemodel.geckolib3.core.controller;

import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationControllerState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.AnimationPoint;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.BoneAnimationQueue;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.TransitionPoint;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.ControllerContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.core.util.MathUtil;
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
    private final ControllerContext ctx;

    @Nullable
    private List<BoneTopLevelSnapshot> modelRendererList;
    @Nullable
    private GeoAnimationController data;
    @Nullable
    private GeoAnimationControllerState state;
    @Nullable
    private String stateName;

    private final ReferenceArrayList<AnimationPlayerHolder> animationPlayers = new ReferenceArrayList<>(8);
    private ReferenceArrayList<BlendBoneAnimationQueue> blendAnimationQueues = new ReferenceArrayList<>(64);
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
        this.ctx = new ControllerContext();
    }

    @Override
    public void process(final float tick, AnimationEvent<T> event, ExpressionEvaluator<MolangContext<?>> evaluator, boolean scheduledUpdate) {
        if (this.data == null) {
            return;
        }

        evaluator.entity().setControllerContext(ctx);

        // 更新状态
        if (this.state == null) {
            var stateName = this.data.initialState();
            var initialState = this.data.states().get(stateName);
            if (initialState == null) {
                return;
            }
            this.stateName = stateName;
            updateState(initialState, evaluator);
        } else {
            for (var transition : state.transitions()) {
                if (!transition.getRight().evalAsBoolean(evaluator)) {
                    continue;
                }
                var stateName = transition.getLeft();
                var newState = this.data.states().get(stateName);
                if (newState == null) {
                    return;
                }
                this.stateName = stateName;
                updateState(newState, evaluator);
                if (activeAnimationPlayerSize == 0) {
                    ctx.setAllAnimationsFinished(true);
                    ctx.setAnyAnimationFinished(true);
                } else {
                    ctx.setAllAnimationsFinished(false);
                    ctx.setAnyAnimationFinished(false);
                }
            }
        }

        // 更新动画
        for (var i = 0; i < this.activeAnimationPlayerSize; i++) {
            var holder = this.animationPlayers.get(i);
            holder.conditionHolder().evaluateApplyCondition(evaluator);
            holder.animationPlayer().process(tick, evaluator, scheduledUpdate, !holder.conditionHolder().shouldApply());
        }

        // 在更新控制器状态之后写入 all_animations_finished 和 any_animation_finished 变量，供下次控制器更新时使用
        if (activeAnimationPlayerSize > 0) {
            ctx.setAnyAnimationFinished(false);
            ctx.setAllAnimationsFinished(true);
            for (var i = 0; i < this.activeAnimationPlayerSize; i++) {
                var holder = this.animationPlayers.get(i);
                if (holder.animationPlayer.currentAnimFinished()) {
                    ctx.setAnyAnimationFinished(true);
                } else {
                    ctx.setAllAnimationsFinished(false);
                }
            }
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getState() {
        return stateName == null ? "(null)" : stateName;
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
        // new 对象比 clear 更加高效
        this.blendAnimationQueues = new ReferenceArrayList<>();
        for (var holder : this.animationPlayers) {
            holder.markAsDirty();
        }
    }

    private void updateState(GeoAnimationControllerState state, ExpressionEvaluator<MolangContext<?>> evaluator) {
        evaluator.entity().setAllowEmitting(true);
        if (this.state != null) {
            for (var exp : this.state.onExit()) {
                exp.eval(evaluator);
            }
        }
        for (var exp : state.onEntry()) {
            exp.eval(evaluator);
        }
        evaluator.entity().setAllowEmitting(false);
        this.state = state;

        // 扩容动画播放器列表
        for (var i = this.animationPlayers.size(); i < state.animations().size(); i++) {
            this.animationPlayers.add(new AnimationPlayerHolder(this.animatableEntity, this.initTransitionLengthTicks));
        }
        // 停用多余的动画播放器
        for (var i = state.animations().size(); i < this.activeAnimationPlayerSize; i++) {
            var player = this.animationPlayers.get(i).animationPlayer();
            player.resetToIdle();
            player.forceReload();
        }
        // 初始化动画播放器
        this.activeAnimationPlayerSize = state.animations().size();
        for (var i = 0; i < state.animations().size(); i++) {
            var holder = this.animationPlayers.get(i);
            var animPair = state.animations().get(i);

            if (holder.isDirty()) {
                holder.animationPlayer().updateRenderer(this.modelRendererList);
                for (var queue : this.blendAnimationQueues) {
                    queue.addUnderlyingQueue(holder.conditionHolder(), holder.animationPlayer().getBoneAnimQueues().get(queue.boneName()));
                }
                holder.clearDirty();
            }

            holder.conditionHolder().setApplyCondition(animPair.getRight());
            holder.animationPlayer().setTransition(state.blendTransition().startNew());
            holder.animationPlayer().setAnimation(animPair.getLeft());
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

    /**
     * 在对过渡动画进行混合时假设所有活跃的动画播放器具有相同的过渡时间和起始点，并且同时开始过渡，
     * 实际上也理应如此。
     */
    private static class BlendBoneAnimationQueue implements IBoneAnimationQueue {
        private final BoneTopLevelSnapshot snapshot;
        private final ReferenceArrayList<Pair<ConditionHolder, BoneAnimationQueue>> underlyingQueues;

        public BlendBoneAnimationQueue(BoneTopLevelSnapshot snapshot) {
            this.snapshot = snapshot;
            this.underlyingQueues = new ReferenceArrayList<>(4);
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
        public Optional<Vector3f> pollRotationPoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
            return pollAndBlend(queue -> queue.rotation, evaluator);
        }

        @Override
        public Optional<Vector3f> pollPositionPoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
            return pollAndBlend(queue -> queue.position, evaluator);
        }

        @Override
        public Optional<Vector3f> pollScalePoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
            return pollAndBlendScale(queue -> queue.scale, evaluator);
        }

        private Optional<Vector3f> pollAndBlend(Function<BoneAnimationQueue, @Nullable AnimationPoint> pointGetter, ExpressionEvaluator<MolangContext<?>> evaluator) {
            var target = new Vector3f();

            boolean active = false;
            boolean first = true;
            boolean isTransition = false;
            Vector3f offset = null;
            float transitionPercentProgress = 0f;

            // 很多内部状态在 getLerpPoint 之后才更新，不要尝试提前初始化上面的变量
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

                if (first) {
                    first = false;
                    if (point instanceof TransitionPoint transitionPoint) {
                        isTransition = true;
                        offset = transitionPoint.getTransitionOffset();
                        transitionPercentProgress = transitionPoint.getTransitionPercentProgress();
                    }
                }

                // 高概率分支尽量放在前面
                if (!isTransition) {
                    var pointValue = point.getLerpPoint(evaluator);
                    target.fma(pair.right().getBlendWeight(), pointValue);
                } else if (transitionPercentProgress <= -0.00001 || transitionPercentProgress >= 0.00001) {
                    var transitionPoint = (TransitionPoint) point;
                    var dst = transitionPoint.getTransitionDst(evaluator);
                    target.fma(pair.right().getBlendWeight(), dst);
                } else {
                    return Optional.of(offset);
                }
            }

            if (active) {
                if (!isTransition) {
                    return Optional.of(target);
                } else {
                    return Optional.of(MathUtil.lerpValues(transitionPercentProgress, offset, target));
                }
            } else {
                return Optional.empty();
            }
        }

        /**
         * scale 的混合比较特殊，它不是累加，而是连乘
         */
        private Optional<Vector3f> pollAndBlendScale(Function<BoneAnimationQueue, @Nullable AnimationPoint> pointGetter, ExpressionEvaluator<MolangContext<?>> evaluator) {
            var target = new Vector3f(1, 1, 1);

            boolean active = false;
            boolean first = true;
            boolean isTransition = false;
            Vector3f offset = null;
            float transitionPercentProgress = 0f;

            // 很多内部状态在 getLerpPoint 之后才更新，不要尝试提前初始化上面的变量

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

                if (first) {
                    first = false;
                    if (point instanceof TransitionPoint transitionPoint) {
                        isTransition = true;
                        offset = transitionPoint.getTransitionOffset();
                        transitionPercentProgress = transitionPoint.getTransitionPercentProgress();
                    }
                }

                // 高概率分支尽量放在前面
                if (!isTransition) {
                    var pointValue = point.getLerpPoint(evaluator);
                    var weight = pair.right().getBlendWeight();
                    if (weight == 1f) {
                        target.mul(pointValue);
                    } else {
                        target.mul(MathUtil.computeWeightedScale(pointValue, weight));
                    }
                } else if (transitionPercentProgress <= -0.00001 || transitionPercentProgress >= 0.00001) {
                    var transitionPoint = (TransitionPoint) point;
                    var dst = transitionPoint.getTransitionDst(evaluator);
                    target.mul(MathUtil.computeWeightedScale(dst, pair.right().getBlendWeight()));
                } else {
                    return Optional.of(offset);
                }
            }

            if (active) {
                if (!isTransition) {
                    return Optional.of(target);
                } else {
                    return Optional.of(MathUtil.lerpValues(transitionPercentProgress, offset, target));
                }
            } else {
                return Optional.empty();
            }
        }
    }
}
