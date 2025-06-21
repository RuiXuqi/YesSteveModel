package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.event.EntityLoadEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DispatchServerDrivenProperty {
    private final int entityId;
    public final byte flying;

    public DispatchServerDrivenProperty(int entityId, byte flying) {
        this.entityId = entityId;
        this.flying = flying;
    }

    public DispatchServerDrivenProperty(Entity entity) {
        entityId = entity.getId();
        if (entity instanceof Player player) {
            flying = player.getAbilities().flying ? (byte)1 : (byte)0;
        } else {
            flying = -1;
        }
    }

    public static void encode(DispatchServerDrivenProperty message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeByte(message.flying);
    }

    public static DispatchServerDrivenProperty decode(FriendlyByteBuf buf) {
        var entityId = buf.readVarInt();
        var flying = buf.readByte();
        return new DispatchServerDrivenProperty(entityId, flying);
    }

    public static void handle(final DispatchServerDrivenProperty msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> {
                var entity = Minecraft.getInstance().level.getEntity(msg.entityId);
                if (entity != null) {
                    handle(entity, msg);
                } else {
                    EntityLoadEvent.addRecoveryHandler(msg.entityId, e -> handle(e, msg));
                }
            });
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(Entity entity, DispatchServerDrivenProperty msg) {
        if (entity instanceof Player player) {
            player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                cap.updateServerDrivenProperty(msg);
            });
        }
    }
}
