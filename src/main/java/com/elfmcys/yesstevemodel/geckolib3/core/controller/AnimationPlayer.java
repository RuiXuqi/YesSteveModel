/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core.controller;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimationState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.transition.IBlendTransition;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.transition.LinearBlendTransition;
import com.elfmcys.yesstevemodel.geckolib3.core.event.InstructionKeyFrameExecutor;
import com.elfmcys.yesstevemodel.geckolib3.core.event.SoundKeyframeExecutor;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.*;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.event.PointType;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
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

import java.util.*;
import java.util.concurrent.TimeUnit;

public class AnimationPlayer {
    /**
     * 首次进入存档时，因为动画不存在会疯狂刷屏。
     * <p>
     * 但是为了方便调试，又必须打印出这段日志。故这里缓存一下同名内容，避免刷屏。
     */
    private static final Cache<String, Object> NOT_EXIST_ANIMATION_NAME_CACHE = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.SECONDS).build();

    /**
     * 模型所有骨骼
     */
    private final Object2ReferenceOpenHashMap<String, BoneAnimationQueue> boneAnimQueues = new Object2ReferenceOpenHashMap<>();
    /**
     * 当前有动画的骨骼
     */
    private final ReferenceArrayList<BoneAnimationQueue> activeBoneAnimQueues = new ReferenceArrayList<>();
    /**
     * 与动画播放相关的 molang 上下文
     */
    private final AnimationContext animationContext = new AnimationContext();
    /**
     * 实体对象
     */
    private final AnimatableEntity<?> animatableEntity;
    /**
     * PLAY_ONCE 动画是否延迟一帧结束播放
     */
    private final boolean delayStop;

    private InstructionKeyFrameExecutor instructionKeyFrameExecutor;
    private SoundKeyframeExecutor soundKeyFrameExecutor;
    /**
     * 在动画之间过渡需要多长时间
     */
    private IBlendTransition transition;
    private float animTickOffset;

    private AnimationState state = AnimationState.IDLE;
    private Pair<ILoopType, String> lastSetAnim = null;
    private Pair<ILoopType, Animation> nextAnim = null;
    private Animation currentAnim;
    private ILoopType currentLoopType;
    private boolean currentAnimFinished = true;

    /**
     * 实例化动画播放器，每个播放器同一时间只能播放一个动画
     *
     * @param animatableEntity      实体
     * @param transitionLengthTicks 动画过渡时间（tick）
     */
    public AnimationPlayer(AnimatableEntity<?> animatableEntity, float transitionLengthTicks, boolean delayStop) {
        this.animatableEntity = animatableEntity;
        this.transition = new LinearBlendTransition(transitionLengthTicks);
        this.animTickOffset = 0.0f;
        this.delayStop = delayStop;
    }

    public void setAnimation(@Nullable String animationName) {
        setAnimation(animationName, null);
    }

    /**
     * 此方法设置当前动画
     * 你可以每帧运行此方法，如果每次都传入相同的动画，它将不会重新启动。
     * 此外，它还可以在动画状态之间平滑过渡
     */
    public void setAnimation(@Nullable String animationName, @Nullable ILoopType loopTypeOverride) {
        if (animationName == null) {
            this.lastSetAnim = null;
            resetToIdle();
            return;
        }

        var animation = animatableEntity.getAnimation(animationName);
        if (animation == null) {
            if (NOT_EXIST_ANIMATION_NAME_CACHE.getIfPresent(animationName) == null) {
                YesSteveModel.LOGGER.warn("Could not load animation: {}. Is it missing?", animationName);
                NOT_EXIST_ANIMATION_NAME_CACHE.put(animationName, animationName);
            }
            return;
        }
        if (loopTypeOverride == null) {
            loopTypeOverride = animation.loop;
        }

        if (lastSetAnim == null || !lastSetAnim.getSecond().equals(animationName) || lastSetAnim.getFirst() != loopTypeOverride) {
            this.lastSetAnim = new Pair<>(loopTypeOverride, animationName);
            this.nextAnim = new Pair<>(loopTypeOverride, animation);
            resetToIdle();
        }
    }

    /**
     * 当前动画，可以为 null
     */
    @Nullable
    public Animation getCurrentAnim() {
        return this.currentAnim;
    }

    /**
     * 当前动画播放器状态
     */
    public AnimationState getState() {
        return this.state;
    }

    public boolean currentAnimFinished() {
        return this.currentAnimFinished;
    }

    public void setTransition(IBlendTransition transition) {
        this.transition = transition;
    }

    /**
     * 当前动画骨骼动画队列
     */
    public Map<String, BoneAnimationQueue> getBoneAnimQueues() {
        return this.boneAnimQueues;
    }

    /**
     * 此方法每帧调用一次，以便填充动画点队列并处理动画状态逻辑。
     *
     * @param entityTicks 当前 tick + 插值 tick
     */
    public void process(final float entityTicks, ExpressionEvaluator<MolangContext<?>> evaluator, boolean scheduledUpdate, boolean dryRun) {
        evaluator.entity().setAnimationContext(animationContext);
        var animTicks = getAnimTicks(entityTicks);

        if ((!delayStop || currentAnimFinished)
                && this.state == AnimationState.RUNNING
                && animTicks > currentAnim.animationLength
                && currentLoopType == ILoopType.EDefaultLoopTypes.PLAY_ONCE) {
            // 当前动画播放结束，清空状态。
            // 使用 currentAnimFinished 作为条件延迟一帧清空状态，
            // 是为了基岩版控制器能正确获取上一个状态的姿态作为过渡动画起始点
            resetEventKeyframes(evaluator, true);
            resetToIdle();
        }

        if (this.state == AnimationState.IDLE) {
            // 没有动画正在播放时，尝试切换下一个动画
            if (!loadNextAnim()) {
                return;
            }

            this.currentAnimFinished = false;
            this.animTickOffset = entityTicks;
            animTicks = 0;

            if (this.transition.length() > 0) {
                this.state = AnimationState.TRANSITIONING;
            } else {
                // 如果过渡长度为 0，直接开始播放
                this.state = AnimationState.RUNNING;
            }
        }

        resetBoneAnimationQueues();

        if (this.state == AnimationState.TRANSITIONING) {
            if (animTicks < this.transition.length()) {
                // 播放过渡动画
                animationContext.setAnimTime(0);
                updateTransition(evaluator, animTicks);
                return;
            } else {
                // 如果当前时间超过了过渡时长，则正式开始播放
                animTicks = animTicks - this.transition.length();
                this.animTickOffset = entityTicks - animTicks;
                this.state = AnimationState.RUNNING;
            }
        }

        // 播放中
        if (this.state == AnimationState.RUNNING) {
            if (animTicks > this.currentAnim.animationLength) {
                if (currentLoopType == ILoopType.EDefaultLoopTypes.LOOP) {
                    // 对于循环动画，本轮播放结束后重置 tick offset，开始下一轮循环
                    if (currentAnim.animationLength > 0) {
                        animTicks = animTicks % currentAnim.animationLength;
                    } else {
                        animTicks = 0;
                    }
                    resetEventKeyframes(evaluator, dryRun);
                    this.animTickOffset = entityTicks - animTicks;
                } else if (currentLoopType == ILoopType.EDefaultLoopTypes.HOLD_ON_LAST_FRAME) {
                    // 停在最后一帧的动画，播放完成后 anim ticks 锁定在最后一帧的时间
                    animTicks = currentAnim.animationLength;
                } else {
                    // PLAY_ONCE 类型在上面就已经处理过了
                }
                this.currentAnimFinished = true;
            }
            animationContext.setAnimTime(animTicks / 20f);

            if (scheduledUpdate) {
                // 更新事件关键帧（指令、音效、粒子等）
                executeEventKeyframes(evaluator, animTicks, dryRun);
            }
            updateAnimation(evaluator, animTicks);
        }
    }

    private void resetEventKeyframes(ExpressionEvaluator<MolangContext<?>> evaluator, boolean dryRun) {
        animationContext.setAnimTime(currentAnim.animationLength / 20f);
        if (this.instructionKeyFrameExecutor != null) {
            this.instructionKeyFrameExecutor.executeRemaining(evaluator, dryRun);
            this.instructionKeyFrameExecutor.reset();
        }
        if (this.soundKeyFrameExecutor != null) {
            this.soundKeyFrameExecutor.reset();
        }
    }

    private void executeEventKeyframes(ExpressionEvaluator<MolangContext<?>> evaluator, float animTicks, boolean dryRun) {
        if (soundKeyFrameExecutor != null) {
            soundKeyFrameExecutor.executeTo(animatableEntity, animTicks, dryRun);
        }
        if (instructionKeyFrameExecutor != null) {
            instructionKeyFrameExecutor.executeTo(evaluator, animTicks, dryRun);
        }
    }

    /**
     * 下次更新时重载当前正在播放的动画
     */
    public void forceReload() {
        this.lastSetAnim = null;
        if (state != AnimationState.IDLE) {
            nextAnim = new Pair<>(currentLoopType, currentAnim);
            resetToIdle();
        }
    }

    private void updateTransition(ExpressionEvaluator<MolangContext<?>> evaluator, float transitionTicks) {
        var blendWeight = currentAnim.blendWeight != null ? currentAnim.blendWeight.evalAsFloat(evaluator) : 1;
        var percentProgress = this.transition.get(transitionTicks);

        for (BoneAnimationQueue boneAnimationQueue : activeBoneAnimQueues) {
            boneAnimationQueue.setBlendWeight(blendWeight);

            BoneSnapshot boneSnapshot = boneAnimationQueue.transitionOffset();
            BoneSnapshot initialSnapshot = boneAnimationQueue.topLevelSnapshot.bone.getInitialSnapshot();

            // 添加即将出现的动画的初始位置，以便模型转换到新动画的初始状态
            if (boneAnimationQueue.rotationKeyFrames != null) {
                boneAnimationQueue.rotation = getTransitionPointAtTick(boneAnimationQueue.rotationKeyFrames, PointType.ROTATION, transitionTicks,
                        new Vector3f(boneSnapshot.rotationValueX - initialSnapshot.rotationValueX,
                                boneSnapshot.rotationValueY - initialSnapshot.rotationValueY,
                                boneSnapshot.rotationValueZ - initialSnapshot.rotationValueZ),
                        percentProgress);
            }

            if (boneAnimationQueue.positionKeyFrames != null) {
                boneAnimationQueue.position = getTransitionPointAtTick(boneAnimationQueue.positionKeyFrames, PointType.POSITION, transitionTicks,
                        new Vector3f(boneSnapshot.positionOffsetX, boneSnapshot.positionOffsetY, boneSnapshot.positionOffsetZ),
                        percentProgress);
            }

            if (boneAnimationQueue.scaleKeyFrames != null) {
                boneAnimationQueue.scale = getTransitionPointAtTick(boneAnimationQueue.scaleKeyFrames, PointType.SCALE, transitionTicks,
                        new Vector3f(boneSnapshot.scaleValueX, boneSnapshot.scaleValueY, boneSnapshot.scaleValueZ),
                        percentProgress);
            }
        }
    }

    private void updateAnimation(ExpressionEvaluator<MolangContext<?>> evaluator, float animTicks) {
        // 循环遍历当前动画中的每个骨骼动画并处理值
        var blendWeight = currentAnim.blendWeight != null ? currentAnim.blendWeight.evalAsFloat(evaluator) : 1;
        for (BoneAnimationQueue boneAnimationQueue : activeBoneAnimQueues) {
            boneAnimationQueue.setBlendWeight(blendWeight);

            if (boneAnimationQueue.rotationKeyFrames != null) {
                boneAnimationQueue.rotation = getKeyFramePointAtTick(boneAnimationQueue.rotationKeyFrames, animTicks);
            }

            if (boneAnimationQueue.positionKeyFrames != null) {
                boneAnimationQueue.position = getKeyFramePointAtTick(boneAnimationQueue.positionKeyFrames, animTicks);
            }

            if (boneAnimationQueue.scaleKeyFrames != null) {
                boneAnimationQueue.scale = getKeyFramePointAtTick(boneAnimationQueue.scaleKeyFrames, animTicks);
            }
        }
    }

    private void resetBoneAnimationQueues() {
        for (BoneAnimationQueue queue : activeBoneAnimQueues) {
            queue.resetQueues();
        }
    }

    public float getAnimTicks(float entityTicks) {
        return Math.max(entityTicks - this.animTickOffset, 0.0f);
    }

    /**
     * 返回当前关键帧播放进度
     **/
    private AnimationPoint getKeyFramePointAtTick(OrderedSegmentSearcher<BoneKeyFrame> frames, float tick) {
        var frame = frames.search(tick);
        return new KeyFramePoint(tick - frame.getStartTick(), frame, animationContext);
    }

    /**
     * 返回过渡进度
     **/
    private TransitionPoint getTransitionPointAtTick(OrderedSegmentSearcher<BoneKeyFrame> frames, PointType type, float tick, Vector3f offsetPoint, float transitionPercentProgress) {
        BoneKeyFrame dstFrame = frames.search(0);
        return new TransitionPoint(tick, transitionPercentProgress, this.transition.length(), offsetPoint, dstFrame, type, animationContext);
    }

    /**
     * 每次给音频关键帧重新赋值时，都需要进行一次清理，停掉先前的音频
     */
    public void stopSoundKeyFrames() {
        if (this.soundKeyFrameExecutor != null) {
            this.soundKeyFrameExecutor.reset();
        }
    }

    /**
     * 尝试加载下个动画
     */
    private boolean loadNextAnim() {
        var next = this.nextAnim;
        if (next == null) {
            return false;
        }
        this.nextAnim = null;

        this.currentAnim = next.getSecond();
        this.currentLoopType = next.getFirst();

        for (BoneAnimation animation : currentAnim.boneAnimations) {
            BoneAnimationQueue queue = boneAnimQueues.get(animation.boneName);
            if (queue == null) {
                continue;
            }
            queue.setBoneAnimation(animation);
            queue.updateTransitionOffset();
            queue.resetQueues();
            queue.setActive(true);
            activeBoneAnimQueues.add(queue);
        }
        instructionKeyFrameExecutor = new InstructionKeyFrameExecutor(currentAnim.customInstructionKeyframes);
        soundKeyFrameExecutor = new SoundKeyframeExecutor(currentAnim.soundKeyFrames);

        return true;
    }

    /**
     * 切换模型，立刻清空所有状态
     */
    public void updateRenderer(List<BoneTopLevelSnapshot> modelRendererList) {
        resetToIdle();
        this.boneAnimQueues.clear();
        this.nextAnim = null;
        this.lastSetAnim = null;

        for (BoneTopLevelSnapshot modelRenderer : modelRendererList) {
            this.boneAnimQueues.put(modelRenderer.name, new BoneAnimationQueue(modelRenderer));
        }
    }

    /**
     * 停止播放动画，重置为待机状态。
     * 注意不会清空 setAnimation 缓存，再次 set “重置之前正在播放的动画”不会生效；
     * 要重新播放重置之前的动画，需要调用 forceReload() 。
     */
    public void resetToIdle() {
        if (this.state != AnimationState.IDLE) {
            this.state = AnimationState.IDLE;
            if (soundKeyFrameExecutor != null) {
                soundKeyFrameExecutor.reset();
            }
            soundKeyFrameExecutor = null;
            instructionKeyFrameExecutor = null;

            for (var queue : this.activeBoneAnimQueues) {
                queue.setActive(false);
            }
            this.activeBoneAnimQueues.clear();

            this.currentAnim = null;
            this.currentAnimFinished = true;
        }
    }
}