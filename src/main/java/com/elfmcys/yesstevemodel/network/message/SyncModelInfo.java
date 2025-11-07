package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.event.EntityLoadEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncModelInfo {
    private final int entityId;
    private final String modelId;
    private final String selectTexture;
    private final DispatchServerDrivenProperty properties;

    public SyncModelInfo(int entityId, String modelId, String selectTexture, DispatchServerDrivenProperty properties) {
        this.entityId = entityId;
        this.modelId = modelId;
        this.selectTexture = selectTexture;
        this.properties = properties;
    }

    public static void encode(SyncModelInfo msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeUtf(msg.modelId);
        buf.writeUtf(msg.selectTexture);
        DispatchServerDrivenProperty.encode(msg.properties, buf);
    }

    public static SyncModelInfo decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        String modelId = buf.readUtf();
        String selectTexture = buf.readUtf();
        var properties = DispatchServerDrivenProperty.decode(buf);
        return new SyncModelInfo(entityId, modelId, selectTexture, properties);
    }

    public static void handle(SyncModelInfo message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            EntityLoadEvent.executeOnEntity(message.entityId,  entity -> handleCapability(entity, message));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleCapability(Entity entity, SyncModelInfo msg) {
        entity.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
            cap.updateModelAndTexture(msg.modelId, msg.selectTexture);
            DispatchServerDrivenProperty.handle(entity, msg.properties);
        });
    }
}
