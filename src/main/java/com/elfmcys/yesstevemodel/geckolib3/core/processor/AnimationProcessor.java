package com.elfmcys.yesstevemodel.geckolib3.core.processor;

import com.elfmcys.yesstevemodel.client.animation.molang.functions.physics.IPhysics;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationData;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IForeignVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.MolangMemory;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.core.util.MathUtil;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class AnimationProcessor<T extends AnimatableEntity<?>> {
    private static final int ROAMING_STRUCT_NAME = StringPool.computeIfAbsent("roaming");

    private final ReferenceArrayList<BoneTopLevelSnapshot> modelRendererList = new ReferenceArrayList<>();
    private final Object2ReferenceOpenHashMap<String, BoneTopLevelSnapshot> modelRendererMap = new Object2ReferenceOpenHashMap<>();
    private final MolangMemory molangMemory = new MolangMemory();
    private final RandomSource random = new XoroshiroRandomSource(RandomSupport.generateUniqueSeed());
    private final DebugInfo debugInfo = new DebugInfo();
    private final ConcurrentLinkedQueue<MolangExecutionTask> pendingMolangTask = new ConcurrentLinkedQueue<>();         // molang 执行任务的生产和消费可能在不同线程上
    private final Object2ReferenceOpenHashMap<String, IPhysics> physicsValues = new Object2ReferenceOpenHashMap<>(16);
    private final T animatable;

    private boolean rendererDirty = false;
    private long cachePhysicsTimeStamp = -1L;

    public AnimationProcessor(T animatable) {
        this.animatable = animatable;
    }

    @SuppressWarnings("unchecked")
    public void tickAnimation(boolean shouldUpdate, AnimationEvent<T> event, MolangContext<?> ctx) {
        ctx.setMemory(this.molangMemory);
        ctx.setRandom(this.random);
        ExpressionEvaluator<MolangContext<?>> evaluator = ExpressionEvaluator.evaluator(ctx);
        preProcess(evaluator);

        // InstancedAnimationFactory 仅保有一个 AnimationData 实例，与传入的 uniqueID 无关
        AnimationData manager = this.animatable.getAnimationData();
        for (IAnimationController<T> controller : manager.getAnimationControllers()) {
            if (this.rendererDirty) {
                controller.updateRenderer(this.modelRendererList);
            }
            // 将当前控制器设置为动画测试事件
            // 处理动画并向点队列添加新值
            controller.process(event, evaluator, shouldUpdate);
            // 解决一个历史遗留问题而保留的动画混合
            @Deprecated boolean blendRotation = controller.blendRotation();
            // 遍历每个骨骼，并对属性进行插值计算
            controller.visitBoneAnimationQueues(boneAnimation -> {
                BoneTopLevelSnapshot snapshot = boneAnimation.getSnapshot();

                boneAnimation.pollRotationPoint(evaluator).ifPresent(rot -> {
                    BoneSnapshot initialSnapshot = snapshot.bone.getInitialSnapshot();
                    @Deprecated Vector3f pointData = snapshot.cachedPointData;
                    if (blendRotation) {
                        pointData.add(rot);
                        initialSnapshot.rotation.add(pointData, snapshot.rotation);
                    } else {
                        pointData.set(rot);
                        initialSnapshot.rotation.add(rot, snapshot.rotation);
                    }
                    snapshot.isCurrentlyRunningRotationAnimation = true;
                });

                boneAnimation.pollPositionPoint(evaluator).ifPresent(position -> {
                    snapshot.position.set(position);
                    snapshot.isCurrentlyRunningPositionAnimation = true;
                });

                boneAnimation.pollScalePoint(evaluator).ifPresent(scale -> {
                    snapshot.scale.set(scale);
                    snapshot.isCurrentlyRunningScaleAnimation = true;
                });
            });
        }

        this.rendererDirty = false;

        // 追踪哪些骨骼应用了动画，并最终将没有动画的骨骼设置为默认值
        final float resetTickLength = manager.getResetSpeed();
        for (BoneTopLevelSnapshot topLevelSnapshot : modelRendererList) {
            BoneSnapshot initialSnapshot = topLevelSnapshot.bone.getInitialSnapshot();

            if (!topLevelSnapshot.isCurrentlyRunningRotationAnimation) {
                float percentageReset = Math.min((event.renderTicks - topLevelSnapshot.mostRecentResetRotationTick) / resetTickLength, 1);
                if (percentageReset >= 1) {
                    MathUtil.lerpValues(percentageReset, topLevelSnapshot.rotation, initialSnapshot.rotation, topLevelSnapshot.rotation);
                }
            } else {
                // FIXME: 2023/7/12 莫名其妙修好了旋转 bug，原因未知
                topLevelSnapshot.mostRecentResetRotationTick = 0;
                topLevelSnapshot.isCurrentlyRunningRotationAnimation = false;
            }

            if (!topLevelSnapshot.isCurrentlyRunningPositionAnimation) {
                float percentageReset = Math.min((event.renderTicks - topLevelSnapshot.mostRecentResetPositionTick) / resetTickLength, 1);
                if (percentageReset >= 1) {
                    MathUtil.lerpValues(percentageReset, topLevelSnapshot.position, initialSnapshot.position, topLevelSnapshot.position);
                }
            } else {
                topLevelSnapshot.mostRecentResetPositionTick = event.renderTicks;
                topLevelSnapshot.isCurrentlyRunningPositionAnimation = false;
            }

            if (!topLevelSnapshot.isCurrentlyRunningScaleAnimation) {
                float percentageReset = Math.min((event.renderTicks - topLevelSnapshot.mostRecentResetScaleTick) / resetTickLength, 1);
                if (percentageReset >= 1) {
                    MathUtil.lerpValues(percentageReset, topLevelSnapshot.scale, initialSnapshot.scale, topLevelSnapshot.scale);
                }
            } else {
                topLevelSnapshot.mostRecentResetScaleTick = event.renderTicks;
                topLevelSnapshot.isCurrentlyRunningScaleAnimation = false;
            }

            topLevelSnapshot.commit();
        }
        manager.isFirstTick = false;

        postProcess(evaluator);
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
        this.molangMemory.initialize(null);
        this.physicsValues.clear();
        this.cachePhysicsTimeStamp = -1L;
        this.rendererDirty = true;
    }

    public void putRemoteStruct(@Nullable Struct remoteStruct) {
        if (remoteStruct != null) {
            molangMemory.setScoped(ROAMING_STRUCT_NAME, remoteStruct);
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

    private void preProcess(ExpressionEvaluator<MolangContext<?>> evaluator) {
        for (var iter = pendingMolangTask.iterator(); iter.hasNext(); ) {
            var task = iter.next();
            if (task.pre) {
                executeMolangTask(task, evaluator);
                iter.remove();
            }
        }
        debugInfo.evaluatePre(evaluator);
    }

    private void postProcess(ExpressionEvaluator<MolangContext<?>> evaluator) {
        float interval;
        long currentTime = Util.getNanos();
        if (cachePhysicsTimeStamp <= 0) {
            interval = 1 / 60f;
        } else {
            interval = Mth.clamp((currentTime - cachePhysicsTimeStamp) / 1000_000_000f, 0f, 1);
        }
        cachePhysicsTimeStamp = currentTime;
        physicsValues.object2ReferenceEntrySet().fastForEach(entry -> entry.getValue().update(interval));

        debugInfo.evaluatePost(evaluator);
        for (var iter = pendingMolangTask.iterator(); iter.hasNext(); ) {
            var task = iter.next();
            if (!task.pre) {
                executeMolangTask(task, evaluator);
                iter.remove();
            }
        }
    }

    private void executeMolangTask(MolangExecutionTask task, ExpressionEvaluator<MolangContext<?>> evaluator) {
        String result;
        try {
            evaluator.entity().setAllowEmitting(task.allowEmitting());
            var ret = task.exp().eval(evaluator);
            if (task.resultCallback() == null) {
                return;
            }
            if (ret == null) {
                result = "null";
            } else if (ret instanceof String) {
                result = "'" + ret + "'";
            } else {
                result = ret.toString();
            }
        } catch (Throwable e) {
            result = "Error: " + e.getMessage();
        } finally {
            evaluator.entity().setAllowEmitting(false);
        }
        task.resultCallback().accept(result);
    }

    public DebugInfo getDebugInfo() {
        return debugInfo;
    }

    public void enqueueMolangTask(IValue value, boolean allowEmitting, boolean pre, @Nullable Consumer<String> resultConsumer) {
        pendingMolangTask.add(new MolangExecutionTask(value, allowEmitting, pre, resultConsumer));
    }

    // 获取公共变量存储（暂未实现）
    public IForeignVariableStorage getPublicVariableStorage() {
        return this.molangMemory;
    }

    public void visitScopedVariableNames(Consumer<String> visitor) {
        this.molangMemory.visitScopedVariableNames(visitor);
    }

    private record MolangExecutionTask(IValue exp, boolean allowEmitting, boolean pre, Consumer<String> resultCallback) {}
}
