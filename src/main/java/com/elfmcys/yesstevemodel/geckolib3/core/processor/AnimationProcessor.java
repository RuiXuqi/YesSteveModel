package com.elfmcys.yesstevemodel.geckolib3.core.processor;

import com.elfmcys.yesstevemodel.client.animation.molang.functions.physics.IPhysics;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationData;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationMolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IForeignVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.VariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.core.util.MathUtil;
import com.elfmcys.yesstevemodel.geckolib3.core.util.RateLimiter;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class AnimationProcessor<T extends AnimatableEntity<?>> {
    private static final int ROAMING_STRUCT_NAME = StringPool.computeIfAbsent("roaming");

    private final ReferenceArrayList<BoneTopLevelSnapshot> modelRendererList = new ReferenceArrayList<>();
    private final Object2ReferenceOpenHashMap<String, BoneTopLevelSnapshot> modelRendererMap = new Object2ReferenceOpenHashMap<>();
    private final VariableStorage animationStorage = new VariableStorage();
    private final Random random = new Random();
    private final DebugInfo debugInfo = new DebugInfo();
    private final ConcurrentLinkedQueue<Pair<IValue, Consumer<String>>> pendingValues = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<String, IPhysics> physicsValues = new ConcurrentHashMap<>();
    private final RateLimiter rateLimiter = new RateLimiter(Minecraft.getInstance().getWindow().getRefreshRate());
    private final T animatable;

    private List<IValue> initializationValues;
    private List<IValue> preAnimationValues;

    private boolean rendererDirty = false;
    private long cachePhysicsTimeStamp = -1L;

    public AnimationProcessor(T animatable) {
        this.animatable = animatable;
    }

    @SuppressWarnings("unchecked")
    public boolean tickAnimation(double seekTime, boolean forceUpdate, AnimationEvent<T> event, AnimationMolangContext<?> ctx) {
        var shouldUpdate = rateLimiter.request((float) (seekTime / 20));
        if (!forceUpdate && !shouldUpdate) {
            return false;
        }

        ctx.setStorage(this.animationStorage);
        ctx.setRandom(this.random);
        ExpressionEvaluator<AnimationMolangContext<?>> evaluator = ExpressionEvaluator.evaluator(ctx);
        preProcess(evaluator);

        // InstancedAnimationFactory 仅保有一个 AnimationData 实例，与传入的 uniqueID 无关
        AnimationData manager = this.animatable.getAnimationData();
        for (IAnimationController<T> controller : manager.getAnimationControllers()) {
            if (this.rendererDirty) {
                controller.updateRenderer(this.modelRendererList);
            }
            // 将当前控制器设置为动画测试事件
            // 处理动画并向点队列添加新值
            controller.process(seekTime, event, evaluator, shouldUpdate);
            // 解决一个历史遗留问题而保留的动画混合
            @Deprecated boolean blendRotation = controller.blendRotation();
            // 遍历每个骨骼，并对属性进行插值计算
            controller.visitBoneAnimationQueues(boneAnimation -> {
                BoneTopLevelSnapshot snapshot = boneAnimation.getSnapshot();

                boneAnimation.pollRotationPoint(evaluator).ifPresent(rot -> {
                    BoneSnapshot initialSnapshot = snapshot.bone.getInitialSnapshot();
                    @Deprecated PointData pointData = snapshot.cachedPointData;
                    pointData.rotationValueX += rot.x();
                    pointData.rotationValueY += rot.y();
                    pointData.rotationValueZ += rot.z();
                    if (blendRotation) {
                        snapshot.rotationValueX = pointData.rotationValueX + initialSnapshot.rotationValueX;
                        snapshot.rotationValueY = pointData.rotationValueY + initialSnapshot.rotationValueY;
                        snapshot.rotationValueZ = pointData.rotationValueZ + initialSnapshot.rotationValueZ;
                    } else {
                        snapshot.rotationValueX = rot.x() + initialSnapshot.rotationValueX;
                        snapshot.rotationValueY = rot.y() + initialSnapshot.rotationValueY;
                        snapshot.rotationValueZ = rot.z() + initialSnapshot.rotationValueZ;
                    }
                    snapshot.isCurrentlyRunningRotationAnimation = true;
                });

                boneAnimation.pollPositionPoint(evaluator).ifPresent(position -> {
                    snapshot.positionOffsetX = position.x();
                    snapshot.positionOffsetY = position.y();
                    snapshot.positionOffsetZ = position.z();
                    snapshot.isCurrentlyRunningPositionAnimation = true;
                });

                boneAnimation.pollScalePoint(evaluator).ifPresent(scale -> {
                    snapshot.scaleValueX = scale.x();
                    snapshot.scaleValueY = scale.y();
                    snapshot.scaleValueZ = scale.z();
                    snapshot.isCurrentlyRunningScaleAnimation = true;
                });
            });
        }

        this.rendererDirty = false;

        // 追踪哪些骨骼应用了动画，并最终将没有动画的骨骼设置为默认值
        final double resetTickLength = manager.getResetSpeed();
        for (BoneTopLevelSnapshot topLevelSnapshot : modelRendererList) {
            BoneSnapshot initialSnapshot = topLevelSnapshot.bone.getInitialSnapshot();

            if (!topLevelSnapshot.isCurrentlyRunningRotationAnimation) {
                double percentageReset = Math.min((seekTime - topLevelSnapshot.mostRecentResetRotationTick) / resetTickLength, 1);
                if (percentageReset >= 1) {
                    topLevelSnapshot.rotationValueX = MathUtil.lerpValues(percentageReset, topLevelSnapshot.rotationValueX,
                            initialSnapshot.rotationValueX);
                    topLevelSnapshot.rotationValueY = MathUtil.lerpValues(percentageReset, topLevelSnapshot.rotationValueY,
                            initialSnapshot.rotationValueY);
                    topLevelSnapshot.rotationValueZ = MathUtil.lerpValues(percentageReset, topLevelSnapshot.rotationValueZ,
                            initialSnapshot.rotationValueZ);
                }
            } else {
                // FIXME: 2023/7/12 莫名其妙修好了旋转 bug，原因未知
                topLevelSnapshot.mostRecentResetRotationTick = 0;
                topLevelSnapshot.isCurrentlyRunningRotationAnimation = false;
            }

            if (!topLevelSnapshot.isCurrentlyRunningPositionAnimation) {
                double percentageReset = Math.min((seekTime - topLevelSnapshot.mostRecentResetPositionTick) / resetTickLength, 1);
                if (percentageReset >= 1) {
                    topLevelSnapshot.positionOffsetX = MathUtil.lerpValues(percentageReset, topLevelSnapshot.positionOffsetX,
                            initialSnapshot.positionOffsetX);
                    topLevelSnapshot.positionOffsetY = MathUtil.lerpValues(percentageReset, topLevelSnapshot.positionOffsetY,
                            initialSnapshot.positionOffsetY);
                    topLevelSnapshot.positionOffsetZ = MathUtil.lerpValues(percentageReset, topLevelSnapshot.positionOffsetZ,
                            initialSnapshot.positionOffsetZ);
                }
            } else {
                topLevelSnapshot.mostRecentResetPositionTick = (float) seekTime;
                topLevelSnapshot.isCurrentlyRunningPositionAnimation = false;
            }

            if (!topLevelSnapshot.isCurrentlyRunningScaleAnimation) {
                double percentageReset = Math.min((seekTime - topLevelSnapshot.mostRecentResetScaleTick) / resetTickLength, 1);
                if (percentageReset >= 1) {
                    topLevelSnapshot.scaleValueX = MathUtil.lerpValues(percentageReset, topLevelSnapshot.scaleValueX, initialSnapshot.scaleValueX);
                    topLevelSnapshot.scaleValueY = MathUtil.lerpValues(percentageReset, topLevelSnapshot.scaleValueY, initialSnapshot.scaleValueY);
                    topLevelSnapshot.scaleValueZ = MathUtil.lerpValues(percentageReset, topLevelSnapshot.scaleValueZ, initialSnapshot.scaleValueZ);
                }
            } else {
                topLevelSnapshot.mostRecentResetScaleTick = (float) seekTime;
                topLevelSnapshot.isCurrentlyRunningScaleAnimation = false;
            }

            topLevelSnapshot.commit();
        }
        manager.isFirstTick = false;

        postProcess(evaluator);
        return true;
    }

    @Nullable
    public IBone getBone(String boneName) {
        BoneTopLevelSnapshot renderer = modelRendererMap.get(boneName);
        return renderer != null ? renderer.bone : null;
    }

    public void registerModelRenderer(Map<String, IBone> boneMap) {
        this.modelRendererMap.clear();
        this.modelRendererList.clear();
        this.modelRendererList.ensureCapacity(boneMap.size());
        for (Map.Entry<String, IBone> entry : boneMap.entrySet()) {
            BoneTopLevelSnapshot renderer = new BoneTopLevelSnapshot(entry.getValue());
            this.modelRendererMap.put(entry.getKey(), renderer);
            this.modelRendererList.add(renderer);
        }
        this.animationStorage.initialize(null);
        this.physicsValues.clear();
        this.cachePhysicsTimeStamp = -1L;
        this.rendererDirty = true;
    }

    public void putRemoteStruct(@Nullable Struct remoteStruct) {
        if (remoteStruct != null) {
            animationStorage.setScoped(ROAMING_STRUCT_NAME, remoteStruct);
        }
    }

    public void putPhysicsValue(String key, IPhysics physics) {
        this.physicsValues.put(key, physics);
    }

    @Nullable
    public IPhysics getPhysicsValue(String key) {
        return this.physicsValues.get(key);
    }

    public boolean isModelRendererEmpty() {
        return modelRendererList.isEmpty();
    }

    private void preProcess(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
        if (rendererDirty && initializationValues != null) {
            for (IValue value : initializationValues) {
                value.evalAsDouble(evaluator);
            }
            initializationValues = null;
        }
        if (preAnimationValues != null) {
            for (IValue value : preAnimationValues) {
                value.evalAsDouble(evaluator);
            }
        }
        debugInfo.evaluatePre(evaluator);
    }

    private void postProcess(ExpressionEvaluator<AnimationMolangContext<?>> evaluator) {
        double interval;
        long currentTime = Util.getNanos();
        if (cachePhysicsTimeStamp <= 0) {
            interval = 1 / 60d;
        } else {
            interval = Mth.clamp((currentTime - cachePhysicsTimeStamp) / 1000_000_000d, 0d, 0.1);
        }
        cachePhysicsTimeStamp = currentTime;
        physicsValues.forEach((key, value) -> value.update(interval));

        debugInfo.evaluatePost(evaluator);
        while (!pendingValues.isEmpty()) {
            Pair<IValue, Consumer<String>> pair = pendingValues.poll();
            String result;
            try {
                var ret = pair.getFirst().evalUnsafe(evaluator);
                if (ret == null) {
                    result = "null";
                } else if (ret instanceof String) {
                    result = "'" + ret + "'";
                } else {
                    result = ret.toString();
                }
            } catch (Exception e) {
                result = "Error: " + e.getMessage();
            }
            if (pair.getSecond() != null) {
                pair.getSecond().accept(result);
            }
        }
    }

    public DebugInfo getDebugInfo() {
        return debugInfo;
    }

    public void execute(IValue value, @Nullable Consumer<String> resultConsumer) {
        pendingValues.add(Pair.of(value, resultConsumer));
    }

    public IForeignVariableStorage getPublicVariableStorage() {
        return this.animationStorage;
    }
}
