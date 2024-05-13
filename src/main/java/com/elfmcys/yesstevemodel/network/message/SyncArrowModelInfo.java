package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.ArrowGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.ArrowModelInfoCapability;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SyncArrowModelInfo {
    private static final Cache<Integer, ArrowModelInfoCapability> PACKET_CACHE = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.SECONDS).build();

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
                PACKET_CACHE.put(message.entityId, message.capability);
            } else {
                handleCapability((AbstractArrow) entity, message.capability);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void recoverFromCache(AbstractArrow arrow) {
        var newCap = PACKET_CACHE.getIfPresent(arrow.getId());
        if (newCap == null) {
            return;
        }
        PACKET_CACHE.invalidate(arrow.getId());
        handleCapability(arrow, newCap);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleCapability(AbstractArrow entity, ArrowModelInfoCapability newCap) {
        entity.getCapability(ArrowGeoCapabilityProvider.CAP).ifPresent(cap -> {
            cap.init(newCap.getOwnerModelId());
        });
    }
}
