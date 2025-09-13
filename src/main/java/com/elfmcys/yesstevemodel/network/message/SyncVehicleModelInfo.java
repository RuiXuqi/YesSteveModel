package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.VehicleAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.VehicleModelInfoCapability;
import com.elfmcys.yesstevemodel.client.event.EntityLoadEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncVehicleModelInfo {
    private final int entityId;
    private final VehicleModelInfoCapability capability;
    private final Int2FloatOpenHashMap molangVarsClientBound;

    public SyncVehicleModelInfo(int entityId, VehicleModelInfoCapability capability, Int2FloatOpenHashMap molangVarsClientBound) {
        this.entityId = entityId;
        this.capability = capability;
        this.molangVarsClientBound = molangVarsClientBound;
    }

    public SyncVehicleModelInfo(int entityId, VehicleModelInfoCapability capability) {
        this(entityId, capability, new Int2FloatOpenHashMap());
    }

    public static void encode(SyncVehicleModelInfo message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeNbt(message.capability.serializeNBT());
    }

    public static SyncVehicleModelInfo decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        CompoundTag compoundTag = buf.readNbt();
        VehicleModelInfoCapability cap = new VehicleModelInfoCapability();
        if (compoundTag != null) {
            cap.deserializeNBT(compoundTag);
        }
        var molangVarsServerBound = cap.getMolangVarsServerBound();
        Int2FloatOpenHashMap molangVarsClientBound = new Int2FloatOpenHashMap();
        molangVarsServerBound.object2FloatEntrySet().fastForEach(entry -> {
            int key = StringPool.computeIfAbsent(entry.getKey());
            molangVarsClientBound.put(key, entry.getFloatValue());
        });
        return new SyncVehicleModelInfo(entityId, cap, molangVarsClientBound);
    }

    public static void handle(SyncVehicleModelInfo message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            EntityLoadEvent.executeOnEntity(message.entityId, entity -> handleCapability(entity, message.capability, message.molangVarsClientBound));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleCapability(Entity entity, VehicleModelInfoCapability newCap, Int2FloatOpenHashMap vars) {
        entity.getCapability(VehicleAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
            cap.init(newCap.getOwnerModelId());
            cap.initRoamingVars(vars);
        });
    }
}
