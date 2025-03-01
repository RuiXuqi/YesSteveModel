/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core.controller;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimationState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.AnimationBuilder;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.transition.IBlendTransition;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.transition.LinearBlendTransition;
import com.elfmcys.yesstevemodel.geckolib3.core.event.InstructionKeyFrameExecutor;
import com.elfmcys.yesstevemodel.geckolib3.core.event.SoundKeyframeExecutor;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.*;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.EasingType;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationMolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.util.OrderedSegmentSearcher;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AnimationPlayer {
    /**
     * 首次进入存档时，因为动画不存在会疯狂刷屏。
     * <p>
     * 但是为了方便调试，又必须打印出这段日志。故这里缓存一下同名内容，避免刷屏。
     */
    private static final Cache<String, String> NOT_EXIST_ANIMATION_NAME_CACHE = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.SECONDS).build();
    private final Object2ReferenceOpenHashMap<String, BoneAnimationQueue> boneAnimationQueues = new Object2ReferenceOpenHashMap<>();
    private final ReferenceArrayList<BoneAnimationQueue> activeBoneAnimationQueues = new ReferenceArrayList<>();
    private boolean rendererDirty = true;

    private InstructionKeyFrameExecutor instructionKeyFrameExecutor;
    private SoundKeyframeExecutor soundKeyFrameExecutor;
    /**
     * 在动画之间过渡需要多长时间
     */
    public IBlendTransition transition;
    public boolean isJustStarting = false;
    public double tickOffset;
    public double animationSpeed = 1D;
    /**
     * 默认情况下，动画将使用关键帧的 EasingType <br>
     * 复写此数值将用于全局
     */
    public EasingType easingType = EasingType.LINEAR;
    /**
     * 实体对象
     */
    protected final AnimatableEntity<?> animatableEntity;
    protected AnimationState animationState = AnimationState.STOPPED;
    protected Queue<Pair<ILoopType, Animation>> animationQueue = new LinkedList<>();
    protected Animation currentAnimation;
    protected ILoopType currentAnimationLoop;
    protected AnimationBuilder currentAnimationBuilder = new AnimationBuilder();
    public boolean shouldResetTick = false;
    protected boolean justStartedTransition = false;
    protected boolean needsAnimationReload = false;
    private boolean justStopped = false;
    /**
     * 当前播放器播放的动画是否已经播放过至少一次了
     * <p>
     * 这个是专为播放器使用的一个变量，因为更新顺序问题，不能直接写入 animationContext
     */
    public boolean animIsFinished = false;

    /**
     * 实例化动画播放器，每个播放器同一时间只能播放一个动画
     *
     * @param animatableEntity      实体
     * @param transitionLengthTicks 动画过渡时间（tick）
     */
    public AnimationPlayer(AnimatableEntity<?> animatableEntity, float transitionLengthTicks) {
        this.animatableEntity = animatableEntity;
        this.transition = new LinearBlendTransition(transitionLengthTicks);
        this.tickOffset = 0.0d;
    }

    /**
     * 实例化动画播放器，每个播放器同一时间只能播放一个动画
     *
     * @param animatableEntity      实体
     * @param transitionLengthTicks 动画过渡时间（tick）
     * @param easingtype            动画过渡插值类型，默认没有
     */
    public AnimationPlayer(AnimatableEntity<?> animatableEntity, float transitionLengthTicks, EasingType easingtype) {
        this.animatableEntity = animatableEntity;
        this.transition = new LinearBlendTransition(transitionLengthTicks);
        this.easingType = easingtype;
        this.tickOffset = 0.0d;
    }

    /**
     * 此方法使用 AnimationBuilder 设置当前动画
     * 你可以每帧运行此方法，如果每次都传入相同的 AnimationBuilder，它将不会重新启动。
     * 此外，它还可以在动画状态之间平滑过渡
     */
    public void setAnimation(AnimationBuilder builder) {
        if (builder == null || builder.getRawAnimationList().isEmpty()) {
            this.animationState = AnimationState.STOPPED;
        } else if (!builder.getRawAnimationList().equals(this.currentAnimationBuilder.getRawAnimationList()) || this.needsAnimationReload) {
            AtomicBoolean encounteredError = new AtomicBoolean(false);
            // 将动画名称列表转换为实际列表，并在此过程中跟踪循环布尔值
            LinkedList<Pair<ILoopType, Animation>> animations = builder.getRawAnimationList().stream().map((rawAnimation) -> {
                Animation animation = animatableEntity.getAnimation(rawAnimation.animationName);
                if (animation == null) {
                    String cacheName = NOT_EXIST_ANIMATION_NAME_CACHE.getIfPresent(rawAnimation.animationName);
                    if (cacheName == null) {
                        YesSteveModel.LOGGER.warn("Could not load animation: {}. Is it missing?", rawAnimation.animationName);
                        NOT_EXIST_ANIMATION_NAME_CACHE.put(rawAnimation.animationName, rawAnimation.animationName);
                    }
                    encounteredError.set(true);
                    return null;
                } else {
                    ILoopType loopType = animation.loop;
                    if (rawAnimation.loopType != null) {
                        loopType = rawAnimation.loopType;
                    }
                    return Pair.of(loopType, animation);
                }
            }).collect(Collectors.toCollection(LinkedList::new));
            if (encounteredError.get()) {
                return;
            }
            this.animationQueue = animations;
            this.currentAnimationBuilder = builder;
            // 下一个动画调用时，将其重置为 0
            this.shouldResetTick = true;
            this.animationState = AnimationState.TRANSITIONING;
            this.justStartedTransition = true;
            this.needsAnimationReload = false;
        }
    }

    /**
     * 当前动画，可以为 null
     */
    @Nullable
    public Animation getCurrentAnimation() {
        return this.currentAnimation;
    }

    /**
     * 当前动画播放器状态
     */
    public AnimationState getAnimationState() {
        return this.animationState;
    }

    /**
     * 当前动画骨骼动画队列
     */
    public Map<String, BoneAnimationQueue> getBoneAnimationQueues() {
        return this.boneAnimationQueues;
    }

    /**
     * 此方法每帧调用一次，以便填充动画点队列并处理动画状态逻辑。
     *
     * @param tick 当前 tick + 插值 tick
     */
    public void process(final double tick, ExpressionEvaluator<AnimationMolangContext<?>> evaluator, boolean scheduledUpdate, boolean dryRun) {
        if (this.currentAnimation != null) {
            Animation animation = animatableEntity.getAnimation(currentAnimation.animationName);
            if (animation != null && this.currentAnimation != animation) {
                this.currentAnimation = animation;
                this.instructionKeyFrameExecutor = new InstructionKeyFrameExecutor(animation.customInstructionKeyframes);
                this.stopSoundKeyFrames();
                this.soundKeyFrameExecutor = new SoundKeyframeExecutor(animation.soundKeyFrames);
            }
        }

        if (this.rendererDirty) {
            this.rendererDirty = false;
            if (currentAnimation != null) {
                switchAnimation();
            }
        }

        double adjustedTick = adjustTick(tick);
        // 过渡结束，重置 tick 并将动画设置为运行
        if (animationQueue.isEmpty() && animationState == AnimationState.TRANSITIONING && adjustedTick >= this.transition.length()) {
            this.shouldResetTick = true;
            this.animationState = AnimationState.RUNNING;
            adjustedTick = adjustTick(tick);
        }
        assert adjustedTick >= 0 : "GeckoLib: Tick was less than zero";

        if (this.currentAnimation == null && this.animationQueue.isEmpty()) {
            stop();
            return;
        }

        if (this.justStartedTransition && (this.shouldResetTick || this.justStopped)) {
            this.justStopped = false;
            adjustedTick = adjustTick(tick);
        } else if (this.currentAnimation == null && !this.animationQueue.isEmpty()) {
            this.shouldResetTick = true;
            this.animationState = AnimationState.TRANSITIONING;
            this.justStartedTransition = true;
            this.needsAnimationReload = false;
            adjustedTick = adjustTick(tick);
        } else if (this.animationState != AnimationState.TRANSITIONING) {
            this.animationState = AnimationState.RUNNING;
        }

        AnimationContext context = new AnimationContext();

        // 处理过渡到其他动画（或仅开始一个动画）
        if (this.animationState == AnimationState.TRANSITIONING) {
            // 刚开始过渡，所以将当前动画设置为第一个
            if (adjustedTick == 0 || this.isJustStarting) {
                this.justStartedTransition = false;
                Pair<ILoopType, Animation> current = animationQueue.poll();
                if (current != null) {
                    this.currentAnimationLoop = current.getFirst();
                    this.currentAnimation = current.getSecond();
                    this.instructionKeyFrameExecutor = new InstructionKeyFrameExecutor(current.getSecond().customInstructionKeyframes);
                    this.stopSoundKeyFrames();
                    this.soundKeyFrameExecutor = new SoundKeyframeExecutor(current.getSecond().soundKeyFrames);
                    resetEventKeyFrames(false, null);
                    switchAnimation();
                } else {
                    this.currentAnimation = null;
                    this.instructionKeyFrameExecutor = null;
                    this.stopSoundKeyFrames();
                    this.soundKeyFrameExecutor = null;
                }
            }
            if (this.currentAnimation != null) {
                context.setAnimTime(0);
                animIsFinished = false;
                resetQueues();

                var blendWeight = currentAnimation.blendWeight != null ? currentAnimation.blendWeight.evalAsDouble(evaluator) : 1;
                for (BoneAnimationQueue boneAnimationQueue : activeBoneAnimationQueues) {
                    boneAnimationQueue.setBlendWeight(blendWeight);

                    BoneSnapshot boneSnapshot = boneAnimationQueue.snapshot();
                    BoneSnapshot initialSnapshot = boneAnimationQueue.topLevelSnapshot.bone.getInitialSnapshot();

                    // 添加即将出现的动画的初始位置，以便模型转换到新动画的初始状态
                    if (boneAnimationQueue.rotationKeyFrames != null) {
                        AnimationPoint point = getTransitionPointAtTick(boneAnimationQueue.rotationKeyFrames, true, adjustedTick,
                                new Vector3f(boneSnapshot.rotationValueX - initialSnapshot.rotationValueX,
                                        boneSnapshot.rotationValueY - initialSnapshot.rotationValueY,
                                        boneSnapshot.rotationValueZ - initialSnapshot.rotationValueZ),
                                context);
                        boneAnimationQueue.rotationQueue().add(point);
                    }

                    if (boneAnimationQueue.positionKeyFrames != null) {
                        AnimationPoint point = getTransitionPointAtTick(boneAnimationQueue.positionKeyFrames, false, adjustedTick,
                                new Vector3f(boneSnapshot.positionOffsetX,
                                        boneSnapshot.positionOffsetY,
                                        boneSnapshot.positionOffsetZ),
                                context);
                        boneAnimationQueue.positionQueue().add(point);
                    }

                    if (boneAnimationQueue.scaleKeyFrames != null) {
                        AnimationPoint point = getTransitionPointAtTick(boneAnimationQueue.scaleKeyFrames, false, adjustedTick,
                                new Vector3f(boneSnapshot.scaleValueX,
                                        boneSnapshot.scaleValueY,
                                        boneSnapshot.scaleValueZ),
                                context);
                        boneAnimationQueue.scaleQueue().add(point);
                    }
                }
            }
        } else if (getAnimationState() == AnimationState.RUNNING) {
            resetQueues();
            // 开始运行动画
            processCurrentAnimation(context, evaluator, adjustedTick, tick, scheduledUpdate, dryRun);
        }
    }

    /**
     * 动画过渡到模型的初始状态
     */
    public void stop() {
        this.animationState = AnimationState.STOPPED;
        this.justStopped = true;
        this.stopSoundKeyFrames();
    }

    private void processCurrentAnimation(AnimationContext context, ExpressionEvaluator<AnimationMolangContext<?>> evaluator, double tick, double actualTick, boolean scheduledUpdate, boolean dryRun) {
        assert currentAnimation != null;
        evaluator.entity().setAnimationContext(context);

        // 如果动画已经结束了
        if (tick >= this.currentAnimation.animationLength) {
            context.setAnimTime(this.currentAnimation.animationLength / 20.0f);
            // 这里多加一个 animIsFinished 的判断，使动画重置在下一帧执行，避免过渡动画被重置
            if (!animIsFinished) {
                animIsFinished = true;
            } else {
                // 如果动画为循环播放，继续重头播放
                if (!this.currentAnimationLoop.isRepeatingAfterEnd()) {
                    // 从队列中提取下一个动画
                    Pair<ILoopType, Animation> peek = this.animationQueue.peek();
                    if (peek == null) {
                        // 没有动画了，那么停止
                        this.animationState = AnimationState.STOPPED;
                        return;
                    } else {
                        // 否则，将状态设置为过渡并开始过渡到下一个动画为下一帧
                        this.animationState = AnimationState.TRANSITIONING;
                        this.shouldResetTick = true;
                        this.currentAnimation = peek.getSecond();
                        this.instructionKeyFrameExecutor = new InstructionKeyFrameExecutor(peek.getSecond().customInstructionKeyframes);
                        this.stopSoundKeyFrames();
                        this.soundKeyFrameExecutor = new SoundKeyframeExecutor(peek.getSecond().soundKeyFrames);
                        this.currentAnimationLoop = peek.getFirst();
                    }
                } else {
                    // 重置 tick，以便下一个动画从刻度 0 开始
                    this.shouldResetTick = true;
                    tick = adjustTick(actualTick);
                    resetEventKeyFrames(true, evaluator);
                }
            }
        }
        context.setAnimTime(tick / 20.0f);

        // 循环遍历当前动画中的每个骨骼动画并处理值
        var blendWeight = currentAnimation.blendWeight != null ? currentAnimation.blendWeight.evalAsDouble(evaluator) : 1;
        for (BoneAnimationQueue boneAnimationQueue : activeBoneAnimationQueues) {
            boneAnimationQueue.setBlendWeight(blendWeight);

            if (boneAnimationQueue.rotationKeyFrames != null) {
                boneAnimationQueue.rotationQueue().add(getKeyFramePointAtTick(boneAnimationQueue.rotationKeyFrames, tick, context));
            }

            if (boneAnimationQueue.positionKeyFrames != null) {
                boneAnimationQueue.positionQueue().add(getKeyFramePointAtTick(boneAnimationQueue.positionKeyFrames, tick, context));
            }

            if (boneAnimationQueue.scaleKeyFrames != null) {
                boneAnimationQueue.scaleQueue().add(getKeyFramePointAtTick(boneAnimationQueue.scaleKeyFrames, tick, context));
            }
        }

        // 计划外更新不执行声音关键帧
        if (soundKeyFrameExecutor != null && scheduledUpdate) {
            soundKeyFrameExecutor.executeTo(animatableEntity, tick, dryRun);
        }

        // 计划外更新不执行指令关键帧
        if (instructionKeyFrameExecutor != null && scheduledUpdate) {
            instructionKeyFrameExecutor.executeTo(evaluator, tick);
        }

        if (this.transition.length() == 0 && shouldResetTick && this.animationState == AnimationState.TRANSITIONING) {
            Pair<ILoopType, Animation> current = animationQueue.poll();
            if (current != null) {
                this.currentAnimation = current.getSecond();
                this.currentAnimationLoop = current.getFirst();
                this.instructionKeyFrameExecutor = new InstructionKeyFrameExecutor(current.getSecond().customInstructionKeyframes);
                this.stopSoundKeyFrames();
                this.soundKeyFrameExecutor = new SoundKeyframeExecutor(current.getSecond().soundKeyFrames);
            } else {
                this.currentAnimation = null;
                this.instructionKeyFrameExecutor = null;
                this.stopSoundKeyFrames();
                this.soundKeyFrameExecutor = null;
            }
        }
    }

    // 在开始新的过渡时，将模型的初始旋转、位置和缩放存储为快照
    private void switchAnimation() {
        clearActiveBoneAnimationQueues();
        for (BoneAnimation animation : currentAnimation.boneAnimations) {
            BoneAnimationQueue queue = boneAnimationQueues.get(animation.boneName);
            if (queue == null) {
                continue;
            }
            queue.setBoneAnimation(animation);
            queue.updateSnapshot();
            queue.resetQueues();
            queue.setActive(true);
            activeBoneAnimationQueues.add(queue);
        }
    }

    // 切换模型，重新填充所有初始动画点队列
    public void updateRenderer(List<BoneTopLevelSnapshot> modelRendererList) {
        this.rendererDirty = true;
        this.boneAnimationQueues.clear();
        for (BoneTopLevelSnapshot modelRenderer : modelRendererList) {
            this.boneAnimationQueues.put(modelRenderer.name, new BoneAnimationQueue(modelRenderer));
        }
        clearActiveBoneAnimationQueues();
        markNeedsReload();
    }

    private void resetQueues() {
        for (BoneAnimationQueue queue : activeBoneAnimationQueues) {
            queue.resetQueues();
        }
    }

    // 在新动画开始、过渡开始或者其他情况下重置 tick
    public double adjustTick(double tick) {
        if (this.shouldResetTick) {
            if (getAnimationState() == AnimationState.TRANSITIONING) {
                this.tickOffset = tick;
            } else if (getAnimationState() == AnimationState.RUNNING) {
                this.tickOffset = tick;
            }
            this.shouldResetTick = false;
            return 0;
        } else {
            return this.animationSpeed * Math.max(tick - this.tickOffset, 0.0D);
        }
    }

    /**
     * 返回当前关键帧播放进度
     **/
    private AnimationPoint getKeyFramePointAtTick(OrderedSegmentSearcher<BoneKeyFrame> frames, double tick, AnimationContext context) {
        var frame = frames.search(tick);
        return new KeyFramePoint(tick - frame.getStartTick(), frame, context);
    }

    /**
     * 返回过渡进度
     **/
    private TransitionPoint getTransitionPointAtTick(OrderedSegmentSearcher<BoneKeyFrame> frames, boolean rotation, double tick, Vector3f offsetPoint, AnimationContext context) {
        BoneKeyFrame dstFrame = frames.search(0);
        return new TransitionPoint(tick, this.transition, offsetPoint, dstFrame, rotation, context);
    }

    private void resetEventKeyFrames(boolean reachEnd, ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
        if (instructionKeyFrameExecutor != null) {
            if (reachEnd) {
                instructionKeyFrameExecutor.executeRemaining(evaluator);
            }
            instructionKeyFrameExecutor.reset();
        }
        this.stopSoundKeyFrames();
    }

    /**
     * 每次给音频关键帧重新赋值时，都需要进行一次清理，停掉先前的音频
     */
    public void stopSoundKeyFrames() {
        if (this.soundKeyFrameExecutor != null) {
            this.soundKeyFrameExecutor.reset();
        }
    }

    public void markNeedsReload() {
        this.needsAnimationReload = true;
        clearActiveBoneAnimationQueues();
    }

    private void clearActiveBoneAnimationQueues() {
        for (var queue : this.activeBoneAnimationQueues) {
            queue.setActive(false);
        }
        this.activeBoneAnimationQueues.clear();
    }

    public void clearAnimationCache() {
        this.currentAnimationBuilder = new AnimationBuilder();
    }

    public double getAnimationSpeed() {
        return this.animationSpeed;
    }

    public void setAnimationSpeed(double animationSpeed) {
        this.animationSpeed = animationSpeed;
    }
}