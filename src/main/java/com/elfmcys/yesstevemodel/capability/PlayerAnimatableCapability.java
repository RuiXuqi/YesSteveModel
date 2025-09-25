package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.client.animation.molang.PhysicsManager;
import com.elfmcys.yesstevemodel.client.animation.molang.roaming.RemoteRoamingStruct;
import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.animation.molang.roaming.LocalRoamingStruct;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.config.ExtraPlayerScreenConfig;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.DebugInfo;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SubmitRoamingVarsChanges;
import com.elfmcys.yesstevemodel.network.message.data.RoamingVarsChanges;
import com.elfmcys.yesstevemodel.util.RenderUtil;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class PlayerAnimatableCapability extends CustomPlayerEntity {
    private final Int2ReferenceOpenHashMap<RemoteStorage> storageMap;
    private final PhysicsManager guiPhysicsManager;
    private final DebugInfo debugInfo;

    private int currentHashShort;
    private Struct roamingStruct;

    public PlayerAnimatableCapability(Player player) {
        super(player, player instanceof LocalPlayer, true);
        storageMap = new Int2ReferenceOpenHashMap<>(8);
        guiPhysicsManager = new PhysicsManager();
        debugInfo = localPlayer ? new DebugInfo() : null;
    }

    @Override
    protected PlayerStateTracker createStateTracker(Player entity) {
        return new PlayerStateTracker(entity, entity instanceof LocalPlayer);
    }

    public PlayerStateTracker getStateTracker() {
        return (PlayerStateTracker) super.getStateTracker();
    }

    private boolean isFirstPersonModActive() {
        return FirstPersonCompat.isInstalled() && FirstPersonCompat.isEnabled();
    }

    @Override
    public boolean canUpdateAsync() {
        // 在 LocalPlayer 第一人称下，如果安装了第一人称模组并启用，或没有禁用纸娃娃，则异步更新是多余的
        return !isLocalPlayer()
                || !Minecraft.getInstance().options.getCameraType().isFirstPerson()
                || (!isFirstPersonModActive() && ExtraPlayerScreenConfig.DISABLE_PLAYER_RENDER.get());
    }

    @Override
    public @Nullable Struct getRoamingStruct() {
        return roamingStruct;
    }

    @Override
    public PhysicsManager getPhysicsManager() {
        if (RenderUtil.isRenderingLevel() || RenderUtil.isRenderingEntitiesInPaperDoll()) {
            return physicsManager;
        } else {
            return guiPhysicsManager;
        }
    }

    @Override
    public void onLoadGeoModel(GeoModelState model) {
        super.onLoadGeoModel(model);
        var hashShort = getModelContainer().info().hashShort();
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
        guiPhysicsManager.reset();
    }

    @Override
    protected void preAnimationSetup(float seekTime) {
        super.preAnimationSetup(seekTime);

        if (getPhysicsManager() == guiPhysicsManager) {
            guiPhysicsManager.update(seekTime);
        }

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
        var storage = this.storageMap.computeIfAbsent(modelHashShort, h -> new RemoteStorage());
        if (isLocalPlayer()) {
            // 对于已成功初始化的 local roaming 丢弃服务端同步
            if (storage.vars == null) {
                storage.vars = vars;
                storage.mergePendingChanges();
                if (modelHashShort == currentHashShort) {
                    // 如果成功初始化，强制重新加载模型
                    roamingStruct = new LocalRoamingStruct(modelHashShort, vars);
                    reloadGeoModel();
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

    public boolean hasRoamingStorage(int hashShort) {
        return storageMap.containsKey(hashShort);
    }

    public void updateRemoteRoamingVars(int modelHashShort, Int2FloatArrayMap vars) {
        // 为了尝试兼容 replay 模组，服务端会额外向 LocalPlayer 发送更新包，非回放时要丢弃
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

    @Override
    @NotNull
    protected HumanoidResourceHolder createResourceHolder(ClientModel model, boolean isFallback) {
        return new HumanoidResourceHolder(model, isFallback, true, true, 30 * 20);
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
