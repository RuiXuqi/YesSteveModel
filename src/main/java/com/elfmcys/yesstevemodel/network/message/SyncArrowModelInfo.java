package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.ArrowGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.ArrowModelInfoCapability;
import com.elfmcys.yesstevemodel.client.event.EntityLoadEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncArrowModelInfo {
    private final int entityId;
    private final ArrowModelInfoCapability capability;

    public SyncArrowModelInfo(int entityId, ArrowModelInfoCapability capability) {
        this.entityId = entityId;
        this.capability = capability;
    }

    public static void encode(SyncArrowModelInfo message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeNbt(message.capability.serializeNBT());
    }

    public static SyncArrowModelInfo decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        CompoundTag compoundTag = buf.readNbt();
        ArrowModelInfoCapability cap = new ArrowModelInfoCapability();
        if (compoundTag != null) {
            cap.deserializeNBT(compoundTag);
        }
        return new SyncArrowModelInfo(entityId, cap);
    }

    public static void handle(SyncArrowModelInfo message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> handlePacket(message));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handlePacket(SyncArrowModelInfo message) {
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
    private static void handleCapability(Entity entity, ArrowModelInfoCapability newCap) {
        entity.getCapability(ArrowGeoCapabilityProvider.CAP).ifPresent(cap -> {
            cap.init(newCap.getOwnerModelId());
        });
    }
}
