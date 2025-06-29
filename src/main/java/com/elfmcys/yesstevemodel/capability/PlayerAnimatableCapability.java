package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.molang.roaming.RemoteRoamingStruct;
import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.animation.molang.roaming.LocalRoamingStruct;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.DispatchServerDrivenProperty;
import com.elfmcys.yesstevemodel.network.message.SubmitRoamingVarsChanges;
import com.elfmcys.yesstevemodel.network.message.data.RoamingVarsChanges;
import it.unimi.dsi.fastutil.ints.Int2FloatArrayMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMaps;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class PlayerAnimatableCapability extends CustomPlayerEntity {
    private final Int2ReferenceOpenHashMap<RemoteStorage> storageMap = new Int2ReferenceOpenHashMap<>(8);
    private int currentHashShort;

    private Struct roamingStruct;
    private boolean remoteFlying;
    private final ConcurrentHashMap<MobEffect, Byte> effects;

    private static float YAW_SPEED;
    private static float LAST_YAW;

    public PlayerAnimatableCapability(AbstractClientPlayer player) {
        super(player, player instanceof LocalPlayer, true);
        effects = new ConcurrentHashMap<>(8);
    }

    private boolean isFirstPersonModActive() {
        if (isLocalPlayer()) {
            return Minecraft.getInstance().options.getCameraType().isFirstPerson() && FirstPersonCompat.isInstalled() && FirstPersonCompat.isEnabled();
        }
        return false;
    }

    @Override
    public boolean canUpdateAsync() {
        // 在第一人称下，如果安装了第一人称模组并启用，则异步更新是多余的
        return !isFirstPersonModActive();
    }

    public static float getLocalPlayerYawSpeed() {
        return YAW_SPEED;
    }

    @Override
    public @Nullable Struct getRoamingStruct() {
        return roamingStruct;
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
    protected void updateFrameData(float currentFrameTime, float lastFrameTime, float partialTicks) {
        if (isLocalPlayer()) {
            updateLocalPlayerYawSpeed(entity, currentFrameTime, lastFrameTime);
        }
        super.updateFrameData(currentFrameTime, lastFrameTime, partialTicks);
    }

    private static void updateLocalPlayerYawSpeed(Entity entity, float currentFrameTime, float lastFrameTime) {
        float yaw = entity.getYRot();
        if (lastFrameTime > 0) {
            YAW_SPEED = (yaw - LAST_YAW) * 1000 / (currentFrameTime - lastFrameTime);
        }
        LAST_YAW = yaw;
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

    public void updateServerDrivenProperty(DispatchServerDrivenProperty msg) {
        if (msg.flying >= 0) {
            remoteFlying = msg.flying != 0;
        }
        if (!msg.effects.isEmpty()) {
            if (msg.full) {
                effects.clear();
            }
            effects.putAll(msg.effects);
        }
    }

    public boolean isFlying() {
        if (isLocalPlayer()) {
            return entity.getAbilities().flying;
        }
        return remoteFlying;
    }

    public byte getEffectLevel(MobEffect effect) {
        if (isLocalPlayer()) {
            var instance = entity.getEffect(effect);
            return instance != null ? (byte) (instance.getAmplifier() + 1) : 0;
        } else {
            return effects.getOrDefault(effect, (byte) 0);
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
