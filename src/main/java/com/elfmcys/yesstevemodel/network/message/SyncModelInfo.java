package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SyncModelInfo {
    private static final Cache<Integer, ModelInfoCapability> PACKET_CACHE = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.SECONDS).build();

    private final int entityId;
    private final ModelInfoCapability capability;

    public SyncModelInfo(int entityId, ModelInfoCapability capability) {
        this.entityId = entityId;
        this.capability = capability;
    }

    public static void encode(SyncModelInfo message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeNbt(message.capability.serializeNBT());
    }

    public static SyncModelInfo decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        CompoundTag compoundTag = buf.readNbt();
        ModelInfoCapability cap = new ModelInfoCapability();
        if (compoundTag != null) {
            cap.deserializeNBT(compoundTag);
        }
        return new SyncModelInfo(entityId, cap);
    }

    public static void handle(SyncModelInfo message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> handlePacket(message));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handlePacket(SyncModelInfo message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(message.entityId);
            if (entity == null) {
                PACKET_CACHE.put(message.entityId, message.capability);
            } else {
                handleCapability(entity, message.capability);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void recoverFromCache(Entity entity) {
        var newCap = PACKET_CACHE.getIfPresent(entity.getId());
        if (newCap == null) {
            return;
        }
        PACKET_CACHE.invalidate(entity.getId());
        handleCapability(entity, newCap);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleCapability(Entity entity, ModelInfoCapability newCap) {
        entity.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
            cap.setModelAndTexture(newCap.getModelId(), newCap.getSelectTexture());
            cap.setRemoteVariables(newCap.getInstanceId(), newCap.getVariables());
            if (newCap.isPlayAnimation()) {
                cap.playAnimation(newCap.getAnimation());
            } else {
                cap.stopAnimation();
            }
        });
    }
}
