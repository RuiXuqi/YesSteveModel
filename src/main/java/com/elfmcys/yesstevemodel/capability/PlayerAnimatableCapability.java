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
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@OnlyIn(Dist.CLIENT)
public final class PlayerAnimatableCapability extends CustomPlayerEntity {
    private final ConcurrentHashMap<Integer, RemoteStorage> storage = new ConcurrentHashMap<>();
    private volatile int currentHashShort;

    private volatile Struct roamingStruct;
    private volatile boolean remoteFlying;
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

    @Override
    public void setupModel(GeoModelState model) {
        super.setupModel(model);
        var hashShort = ClientModelManager.getModel(getModelId()).map(ClientModel::modelInfo).orElseThrow().hashShort();
        currentHashShort = hashShort;
        // 切换模型后如果没有本地缓存，在服务端 roaming 下发之前需要丢弃本地更改
        storage.compute(hashShort, (hash, storage) -> {
            if (storage != null) {
                if (storage.vars != null) {
                    if (isLocalPlayer()) {
                        roamingStruct = new LocalRoamingStruct(hashShort, storage.vars);
                    } else {
                        roamingStruct = new RemoteRoamingStruct(storage.vars);
                    }
                } else {
                    roamingStruct = null;
                }
                return storage;
            } else {
                roamingStruct = null;
                return new RemoteStorage();
            }
        });
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

    public static float getLocalPlayerYawSpeed() {
        return YAW_SPEED;
    }

    public void resetRoamingVars(int modelHashShort, Int2FloatOpenHashMap vars) {
        this.storage.compute(currentHashShort, (h, storage) -> {
            if (storage != null) {
                if (storage.vars == null) {
                    storage.vars = vars;
                }
                return storage;
            } else {
                var newStorage = new RemoteStorage();
                newStorage.vars = vars;
                return newStorage;
            }
        });
        // 检查当前模型是否匹配
        if (modelHashShort == currentHashShort) {
            if (isLocalPlayer()) {
                // 如果 local roaming 已成功初始化，丢弃服务端同步
                if (this.roamingStruct instanceof LocalRoamingStruct) {
                    return;
                }
                // 如果成功初始化，强制重新加载模型
                roamingStruct = new LocalRoamingStruct(modelHashShort, vars);
                updateCurrentModel(true);
            } else {
                roamingStruct = new RemoteRoamingStruct(vars);
            }
        }
    }

    public void updateRemoteRoamingVars(int modelHashShort, Int2FloatArrayMap vars) {
        // 为了尝试兼容 replay 模组，服务端会额外向 LocalPlayer 发送更新包，非回放时要丢弃
        if (!isLocalPlayer() && !vars.isEmpty()) {
            storage.compute(modelHashShort, (h, storage) -> {
                if (storage != null) {
                    storage.pendingChangedVars.add(vars);
                    return storage;
                } else {
                    var newStorage = new RemoteStorage();
                    newStorage.pendingChangedVars.add(vars);
                    return newStorage;
                }
            });
        }
    }

    @Override
    public @Nullable Struct getRoamingStruct() {
        if (roamingStruct instanceof RemoteRoamingStruct struct && currentHashShort != 0) {
            storage.compute(currentHashShort, (h, storage) -> {
                if (storage != null) {
                    var vars = storage.pendingChangedVars;
                    while (true) {
                        var changes = vars.poll();
                        if (changes == null) {
                            break;
                        }
                        struct.update(changes);
                    }
                    return storage;
                } else {
                    return new RemoteStorage();
                }
            });
        }
        return roamingStruct;
    }

    public void handleRoamingVarsChanges() {
        // 此处在主线程上调用，并行范围被限定在实体渲染的循环内，应该是安全的
        if (isLocalPlayer() && this.currentHashShort != 0) {
            if (this.roamingStruct instanceof LocalRoamingStruct localRoamingStruct && localRoamingStruct.isDirty()) {
                var changes = localRoamingStruct.popChanges();
                var variables = new Object2FloatArrayMap<String>(changes.variables.size());
                changes.variables.int2FloatEntrySet().fastForEach(entry -> {
                    var nameStr = StringPool.getString(entry.getIntKey());
                    if (nameStr.length() <= LocalRoamingStruct.MAX_NAME_LENGTH) {
                        variables.put(nameStr, entry.getFloatValue());
                    }
                });
                if (!variables.isEmpty()) {
                    var msg = new RoamingVarsChanges(this.currentHashShort, variables, null, this.entity.getId());
                    NetworkHandler.sendToServer(new SubmitRoamingVarsChanges(msg));
                }
            }
        }
    }

    public void updateServerDrivenProperty(DispatchServerDrivenProperty msg) {
        if (msg.flying >= 0) {
            remoteFlying = msg.flying != 0;
        }
        if (!msg.effects.isEmpty()) {
            if (msg.effects.size() > 1) {
                // 全量同步
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
        public final ConcurrentLinkedQueue<Int2FloatArrayMap> pendingChangedVars = new ConcurrentLinkedQueue<>();
    }
}
