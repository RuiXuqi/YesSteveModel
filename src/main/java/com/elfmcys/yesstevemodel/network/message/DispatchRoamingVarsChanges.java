package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TlmCommonCompat;
import com.elfmcys.yesstevemodel.client.event.EntityLoadEvent;
import com.elfmcys.yesstevemodel.network.message.data.RoamingVarsChanges;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DispatchRoamingVarsChanges {
    public final RoamingVarsChanges changes;

    public DispatchRoamingVarsChanges(RoamingVarsChanges changes) {
        this.changes = changes;
    }

    public static void encode(DispatchRoamingVarsChanges message, FriendlyByteBuf buf) {
        RoamingVarsChanges.encode(message.changes, buf);
    }

    public static DispatchRoamingVarsChanges decode(FriendlyByteBuf buf) {
        var changes = RoamingVarsChanges.decode(buf, true);
        return new DispatchRoamingVarsChanges(changes);
    }

    public static void handle(final DispatchRoamingVarsChanges msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            EntityLoadEvent.executeOnEntity(msg.changes.entityId, entity -> handle(entity, msg));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handle(Entity entity, DispatchRoamingVarsChanges msg) {
        if (TlmCommonCompat.isMaid(entity)) {
            TlmCommonCompat.handleVariableChanges(entity, msg.changes);
        } else if (entity instanceof Player player) {
            player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                cap.updateRemoteRoamingVars(msg.changes.modelHashShort, msg.changes.variablesClientBound);
            });
        }
    }
}
