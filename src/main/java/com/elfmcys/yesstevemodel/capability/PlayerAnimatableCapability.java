package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.debug.CustomDebugSource;
import com.elfmcys.yesstevemodel.client.animation.molang.roaming.RemoteRoamingStruct;
import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.animation.molang.roaming.LocalRoamingStruct;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.config.ExtraPlayerScreenConfig;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.DebugInfo;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SubmitRoamingVarsChanges;
import com.elfmcys.yesstevemodel.network.message.data.RoamingVarsChanges;
import it.unimi.dsi.fastutil.ints.Int2FloatArrayMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMaps;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class PlayerAnimatableCapability extends CustomPlayerEntity {
    private final Int2ReferenceOpenHashMap<RemoteStorage> storageMap = new Int2ReferenceOpenHashMap<>(8);
    private final DebugInfo debugInfo;

    private int currentHashShort;
    private Struct roamingStruct;

    public PlayerAnimatableCapability(Player player) {
        super(player, player instanceof LocalPlayer, true);
        debugInfo = localPlayer ? new DebugInfo() : null;
    }

    @Override
    protected PlayerStateTracker createStateTracker(Player entity) {
        return new PlayerStateTracker(entity, entity instanceof LocalPlayer);
    }

    public PlayerStateTracker getStateTracker() {
        return (PlayerStateTracker) super.getStateTracker();
    }

    @Override
    public DebugSource getDebugSource() {
        if (DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE) {
            return CustomDebugSource.INSTANCE;
        } else {
            return null;
        }
    }

    private boolean isFirstPersonModActive() {
        if (isLocalPlayer()) {
            return FirstPersonCompat.isInstalled() && FirstPersonCompat.isEnabled();
        }
        return false;
    }

    @Override
    public boolean canUpdateAsync() {
        // 在第一人称下，如果安装了第一人称模组并启用，或没有禁用纸娃娃，则异步更新是多余的
        return !Minecraft.getInstance().options.getCameraType().isFirstPerson() || (!isFirstPersonModActive() && ExtraPlayerScreenConfig.DISABLE_PLAYER_RENDER.get());
    }

    @Override
    public @Nullable Struct getRoamingStruct() {
        return roamingStruct;
    }

    @Override
    protected boolean allowEmitting() {
        // 同一帧内只有第一次更新允许生成行为
        return currentFrameRenderTimes == 1;
    }

    @Override
    public void setupModel(GeoModelState model) {
        super.setupModel(model);
        var hashShort = ClientModelManager.getModel(getModelId()).map(ClientModel::modelInfo).orElseThrow().hashShort();
        currentHashShort = hashShort;
        // 切换模型后如果没有本地缓存，在服务端 roaming 下发之前需要丢弃本地更改
        var storage = storageMap.get(hashShort);
        if (storage != null && storage.vars != null) {
            if (isLocalPlayer()) {
                roamingStruct = new LocalRoamingStruct(hashShort, storage.vars);
            } else {
                roamingStruct = new RemoteRoamingStruct(storage.vars);
            }
        } else {
            roamingStruct = null;
        }
    }

    @Override
    protected void preAnimationSetup(float seekTime) {
        super.preAnimationSetup(seekTime);

        // 更新调试信息
        if (debugInfo != null && debugInfo.isEnabled()) {
            var processor = getAnimationProcessor();
            processor.enqueueMolangTask(evaluator -> {
                debugInfo.evaluatePre(evaluator);
                return null;
            }, false, true, null);
            processor.enqueueMolangTask(evaluator -> {
                debugInfo.evaluatePost(evaluator);
                return null;
            }, false, false, null);
        }
    }

    public DebugInfo getDebugInfo() {
        return debugInfo;
    }

    public void resetRoamingVars(int modelHashShort, Int2FloatOpenHashMap vars) {
        waitForAsyncUpdate();
        var storage = this.storageMap.computeIfAbsent(modelHashShort, h -> new RemoteStorage());
        if (isLocalPlayer()) {
            // 对于已成功初始化的 local roaming 丢弃服务端同步
            if (storage.vars == null) {
                storage.vars = vars;
                storage.mergePendingChanges();
                if (modelHashShort == currentHashShort) {
                    // 如果成功初始化，强制重新加载模型
                    roamingStruct = new LocalRoamingStruct(modelHashShort, vars);
                    updateCurrentModel(true);
                }
            }
        } else {
            // 如果是 remote roaming 则无条件同步
            storage.vars = vars;
            storage.mergePendingChanges();
            if (modelHashShort == currentHashShort) {
                roamingStruct = new RemoteRoamingStruct(vars);
            }
        }
    }

    public void updateRemoteRoamingVars(int modelHashShort, Int2FloatArrayMap vars) {
        // 为了尝试兼容 replay 模组，服务端会额外向 LocalPlayer 发送更新包，非回放时要丢弃
        waitForAsyncUpdate();
        if (!isLocalPlayer() && !vars.isEmpty()) {
            var storage = storageMap.computeIfAbsent(modelHashShort, h -> new RemoteStorage());
            if (storage.vars != null) {
                storage.vars.putAll(vars);
            } else {
                storage.pendingChanges.enqueue(vars);
            }
        }
    }

    public void handleRoamingVarsChanges() {
        waitForAsyncUpdate();
        if (isLocalPlayer() && this.currentHashShort != 0
                && this.roamingStruct instanceof LocalRoamingStruct localRoamingStruct
                && localRoamingStruct.isDirty()) {
            var changes = localRoamingStruct.popChanges();
            var nameArray = new String[changes.variables.size()];
            var valueArray = new float[changes.variables.size()];
            int i = 0;
            for (var entry : Int2FloatMaps.fastIterable(changes.variables)) {
                var nameStr = StringPool.getString(entry.getIntKey());
                if (nameStr.length() <= LocalRoamingStruct.MAX_NAME_LENGTH) {
                    nameArray[i] = nameStr;
                    valueArray[i] = entry.getFloatValue();
                } else {
                    nameArray[i] = "";
                    valueArray[i] = 0f;
                }
                ++i;
            }
            var msg = new RoamingVarsChanges(this.currentHashShort, new Object2FloatArrayMap<>(nameArray, valueArray), null, this.entity.getId());
            NetworkHandler.sendToServer(new SubmitRoamingVarsChanges(msg));
        }
    }

    private static class RemoteStorage {
        public volatile Int2FloatOpenHashMap vars;
        public final ObjectArrayFIFOQueue<Int2FloatArrayMap> pendingChanges = new ObjectArrayFIFOQueue<>(4);

        public void mergePendingChanges() {
            while (!pendingChanges.isEmpty()) {
                var pendingVars = pendingChanges.dequeue();
                vars.putAll(pendingVars);
            }
        }
    }
}
