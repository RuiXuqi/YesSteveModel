package com.elfmcys.ysm.network.message;

import com.elfmcys.ysm.capability.ClientLazyCapabilityProvider;
import com.elfmcys.ysm.capability.ProjectileModelInfoCapability;
import com.elfmcys.ysm.client.event.EntityLoadEvent;
import com.elfmcys.ysm.geckolib3.core.molang.util.StringPool;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncProjectileModelInfo {
    private final int entityId;
    private final ProjectileModelInfoCapability capability;
    private final Int2FloatOpenHashMap molangVarsClientBound;

    public SyncProjectileModelInfo(int entityId, ProjectileModelInfoCapability capability, Int2FloatOpenHashMap molangVarsClientBound) {
        this.entityId = entityId;
        this.capability = capability;
        this.molangVarsClientBound = molangVarsClientBound;
    }

    public SyncProjectileModelInfo(int entityId, ProjectileModelInfoCapability capability) {
        this(entityId, capability, new Int2FloatOpenHashMap());
    }

    public static void encode(SyncProjectileModelInfo message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeNbt(message.capability.serializeNBT());
    }

    public static SyncProjectileModelInfo decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        CompoundTag compoundTag = buf.readNbt();
        ProjectileModelInfoCapability cap = new ProjectileModelInfoCapability();
        if (compoundTag != null) {
            cap.deserializeNBT(compoundTag);
        }
        var molangVarsServerBound = cap.getMolangVarsServerBound();
        Int2FloatOpenHashMap molangVarsClientBound = new Int2FloatOpenHashMap();
        molangVarsServerBound.object2FloatEntrySet().fastForEach(entry -> {
            int key = StringPool.computeIfAbsent(entry.getKey());
            molangVarsClientBound.put(key, entry.getFloatValue());
        });
        return new SyncProjectileModelInfo(entityId, cap, molangVarsClientBound);
    }

    public static void handle(SyncProjectileModelInfo message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            EntityLoadEvent.executeOnEntity(message.entityId, entity -> handleCapability(entity, message.capability, message.molangVarsClientBound));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("DataFlowIssue")
    private static void handleCapability(Entity entity, ProjectileModelInfoCapability newCap, Int2FloatOpenHashMap vars) {
        entity.getCapability(ClientLazyCapabilityProvider.CAP).ifPresent(cap -> {
            var animatable = cap.getProjectileAnimatableCapabilityProvider().initialize();
            animatable.init(newCap.getOwnerModelId());
            animatable.initRoamingVars(vars);
        });
    }
}
