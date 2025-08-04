package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.ProjectileAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.ProjectileModelInfoCapability;
import com.elfmcys.yesstevemodel.client.event.EntityLoadEvent;
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

    public SyncProjectileModelInfo(int entityId, ProjectileModelInfoCapability capability) {
        this.entityId = entityId;
        this.capability = capability;
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
        return new SyncProjectileModelInfo(entityId, cap);
    }

    public static void handle(SyncProjectileModelInfo message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            EntityLoadEvent.executeOnEntity(message.entityId, entity -> handleCapability(entity, message.capability));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleCapability(Entity entity, ProjectileModelInfoCapability newCap) {
        entity.getCapability(ProjectileAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
            cap.init(newCap.getOwnerModelId());
        });
    }
}
