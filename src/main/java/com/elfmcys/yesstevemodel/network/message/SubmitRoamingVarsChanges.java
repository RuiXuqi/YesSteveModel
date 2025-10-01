package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.VehicleModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TlmCommonCompat;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.data.RoamingVarsChanges;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SubmitRoamingVarsChanges {
    public final RoamingVarsChanges changes;

    public SubmitRoamingVarsChanges(RoamingVarsChanges changes) {
        this.changes = changes;
    }

    public static void encode(SubmitRoamingVarsChanges message, FriendlyByteBuf buf) {
        RoamingVarsChanges.encode(message.changes, buf);
    }

    public static SubmitRoamingVarsChanges decode(FriendlyByteBuf buf) {
        return new SubmitRoamingVarsChanges(RoamingVarsChanges.decode(buf, false));
    }

    public static void handle(final SubmitRoamingVarsChanges message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer() && context.getSender() != null) {
            var sender = context.getSender();
            NetworkHandler.broadcastToVisiblePlayersAndSelf(new DispatchRoamingVarsChanges(message.changes), sender);
            context.enqueueWork(() -> handle(message, sender.serverLevel()));
        }
        context.setPacketHandled(true);
    }

    private static void handle(SubmitRoamingVarsChanges message, ServerLevel level) {
        Entity entity = level.getEntity(message.changes.entityId);
        if (TlmCommonCompat.isMaid(entity)) {
            TlmCommonCompat.handleVariableChanges(entity, message.changes);
        } else if (entity instanceof ServerPlayer player) {
            player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(cap -> {
                cap.updateRoamingVars(message.changes);
                if (player.getVehicle() != null) {
                    player.getVehicle().getCapability(VehicleModelInfoCapabilityProvider.CAP).ifPresent(vehicleCap -> {
                        cap.getMolangVars().ifPresent(molangVars -> {
                            vehicleCap.update(cap.getModelId(), molangVars);
                        });
                    });
                }
            });
        }
    }
}
