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

    public PlayerAnimatableCapability(AbstractClientPlayer player) {
        super(player, player instanceof LocalPlayer, true);
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
        var remoteStorage = storage.get(hashShort);
        if (remoteStorage != null && remoteStorage.vars != null) {
            if (isLocalPlayer()) {
                roamingStruct = new LocalRoamingStruct(hashShort, remoteStorage.vars);
            } else {
                roamingStruct = new RemoteRoamingStruct(remoteStorage.vars);
            }
        }
    }

    public void resetRoamingVars(int modelHashShort, Int2FloatOpenHashMap vars) {
        var struct = isLocalPlayer()
                ? new LocalRoamingStruct(modelHashShort, vars)
                : new RemoteRoamingStruct(vars);
        this.storage.computeIfAbsent(currentHashShort, h -> new RemoteStorage()).vars = vars;
        this.roamingStruct = struct;
    }

    public void updateRemoteRoamingVars(int modelHashShort, Int2FloatArrayMap vars) {
        // 为了尝试兼容 replay 模组，服务端会额外向 LocalPlayer 发送更新包，非回放时要丢弃
        if (!isLocalPlayer() && !vars.isEmpty()) {
            storage.computeIfAbsent(modelHashShort, h -> new RemoteStorage()).pendingChangedVars.add(vars);
        }
    }

    @Override
    public @Nullable Struct getRoamingStruct() {
        if (roamingStruct instanceof RemoteRoamingStruct struct && currentHashShort != 0) {
            var pendingVars = storage.computeIfAbsent(currentHashShort, h -> new RemoteStorage()).pendingChangedVars;
            while (true) {
                var changes = pendingVars.poll();
                if (changes == null) {
                    break;
                }
                struct.update(changes);
            }
        }
        return roamingStruct;
    }

    public void handlePlayerStateChanges() {
        if (isLocalPlayer() && this.currentHashShort != 0) {
            if (this.roamingStruct instanceof LocalRoamingStruct localRoamingStruct && localRoamingStruct.isDirty()) {
                var vars = localRoamingStruct.popChanges();
                var variables = new Object2FloatArrayMap<String>();
                for (var entry : vars.variables.entrySet()) {
                    var nameStr = StringPool.getString(entry.getKey());
                    if (nameStr.length() <= LocalRoamingStruct.MAX_NAME_LENGTH) {
                        variables.put(nameStr, entry.getValue().floatValue());
                    }
                }
                if (!variables.isEmpty()) {
                    var changes = new RoamingVarsChanges(this.currentHashShort, variables, null, this.entity.getId());
                    NetworkHandler.sendToServer(new SubmitRoamingVarsChanges(changes));
                }
            }
        }
    }

    public void updateServerDrivenProperty(DispatchServerDrivenProperty msg) {
        if (msg.flying >= 0) {
            remoteFlying = msg.flying != 0;
        }
    }

    public boolean isFlying() {
        if (isLocalPlayer()) {
            return entity.getAbilities().flying;
        }
        return remoteFlying;
    }

    private static class RemoteStorage {
        public volatile Int2FloatOpenHashMap vars;
        public final ConcurrentLinkedQueue<Int2FloatArrayMap> pendingChangedVars = new ConcurrentLinkedQueue<>();
    }
}
