package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.ProjectileAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.ProjectileModelInfoCapability;
import com.elfmcys.yesstevemodel.client.event.EntityLoadEvent;
import net.minecraft.client.Minecraft;
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
            context.enqueueWork(() -> handlePacket(message));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handlePacket(SyncProjectileModelInfo message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(message.entityId);
            if (entity == null) {
                EntityLoadEvent.addRecoveryHandler(message.entityId, e -> handleCapability(e, message.capability));
            } else {
                handleCapability(entity, message.capability);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleCapability(Entity entity, ProjectileModelInfoCapability newCap) {
        entity.getCapability(ProjectileAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
            cap.init(newCap.getOwnerModelId());
        });
    }
}
