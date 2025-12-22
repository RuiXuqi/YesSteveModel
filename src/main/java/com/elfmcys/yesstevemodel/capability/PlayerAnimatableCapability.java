package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.client.animation.molang.roaming.LocalRoamingStruct;
import com.elfmcys.yesstevemodel.client.animation.molang.roaming.RemoteRoamingStruct;
import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.compat.bettercombat.BetterCombatCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SubmitRoamingVarsChanges;
import com.elfmcys.yesstevemodel.network.message.data.RoamingVarsChanges;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMaps;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class PlayerAnimatableCapability extends CustomPlayerEntity {
    private final Int2ReferenceOpenHashMap<RemoteStorage> storageMap;

    private int currentHashShort;
    private Struct roamingStruct;

    public PlayerAnimatableCapability(Player player) {
        super(player, player instanceof LocalPlayer, true);
        storageMap = new Int2ReferenceOpenHashMap<>(8);
    }

    @Override
    protected PlayerStateTracker createStateTracker(Player entity) {
        return new PlayerStateTracker(entity, entity instanceof LocalPlayer);
    }

    public PlayerStateTracker getStateTracker() {
        return (PlayerStateTracker) super.getStateTracker();
    }

    @Override
    public @Nullable Struct getRoamingStruct() {
        return roamingStruct;
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
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    protected void codeAnimation(AnimationEvent<? extends AnimatableEntity<Player>> animationEvent, boolean update) {
        super.codeAnimation(animationEvent, update);

        // 更新第一人称相机偏移与头部隐藏
        GeoModelState model = getLoadedGeoModel();
        if (model != null && isLocalPlayer()) {
            if (!animationEvent.isRenderingInLevelExclusive() && FirstPersonCompat.isInstalled()) {
                if (model.firstPersonHead() != null) {
                    model.firstPersonHead().setHidden(FirstPersonCompat.shouldHideHead());
                }
                if (model.firstPersonViewLocator() != null) {
                    FirstPersonCompat.setHeadPos(model.firstPersonViewLocator().getPivotY() * getHeightScale());
                } else if (update) {
                    if (!model.headBones().isEmpty()) {
                        var head = model.headBones().get(model.headBones().size() - 1);
                        FirstPersonCompat.setHeadPos(head == null ? 24f : (head.getPivotY() * getHeightScale()));
                    }
                }
            }
        }
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    protected void recoverLastCodedAnimation(boolean lastFrameUpdated) {
        super.recoverLastCodedAnimation(lastFrameUpdated);

        GeoModelState model = getLoadedGeoModel();
        if (model != null && isLocalPlayer()) {
            if ((FirstPersonCompat.isInstalled() || BetterCombatCompat.isInstalled()) && model.firstPersonHead() != null) {
                model.firstPersonHead().setHidden(false);
            }
        }
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

    private void updateVehicleRoamingVars(int modelHashShort, Int2FloatMap vars) {
        if (modelHashShort == currentHashShort && entity.getVehicle() != null && entity.getVehicle().getFirstPassenger() == entity) {
            entity.getVehicle().getCapability(VehicleAnimatableCapabilityProvider.CAP).ifPresent(vehicleCap -> {
                vehicleCap.updateRoamingVars(vars);
            });
        }
    }

    public void updateRemoteRoamingVars(int modelHashShort, Int2FloatMap vars) {
        // 为了尝试兼容 replay 模组，服务端会额外向 LocalPlayer 发送更新包，非回放时要丢弃
        if (!isLocalPlayer() && !vars.isEmpty()) {
            var storage = storageMap.computeIfAbsent(modelHashShort, h -> new RemoteStorage());
            if (storage.vars != null) {
                storage.vars.putAll(vars);
            } else {
                storage.pendingChanges.enqueue(vars);
            }
            updateVehicleRoamingVars(modelHashShort, vars);
        }
    }

    public void handleRoamingVarsChanges() {
        if (isLocalPlayer() && this.currentHashShort != 0
            && this.roamingStruct instanceof LocalRoamingStruct localRoamingStruct
            && localRoamingStruct.isDirty()) {
            var changes = localRoamingStruct.popChanges();
            updateVehicleRoamingVars(changes.modelHashShort, changes.variables);
            
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

    public void copyFrom(PlayerAnimatableCapability source) {
        this.storageMap.putAll(source.storageMap);
        updateModelAndTexture(source.getModelId(), source.textureName);
        this.setDisabled(source.isDisabled());
        source.storageMap.clear();
        source.roamingStruct = null;
    }

    @Override
    @NotNull
    protected HumanoidResourceHolder createResourceHolder(ClientModel model, boolean isFallback) {
        return new HumanoidResourceHolder(model, isFallback, true, true, 30 * 20);
    }

    private static class RemoteStorage {
        public volatile Int2FloatOpenHashMap vars;
        public final ObjectArrayFIFOQueue<Int2FloatMap> pendingChanges = new ObjectArrayFIFOQueue<>(4);

        public void mergePendingChanges() {
            while (!pendingChanges.isEmpty()) {
                var pendingVars = pendingChanges.dequeue();
                vars.putAll(pendingVars);
            }
        }
    }
}
