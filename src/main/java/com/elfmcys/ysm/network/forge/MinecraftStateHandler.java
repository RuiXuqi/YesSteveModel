package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.capability.ClientLazyCapabilityProvider;
import com.elfmcys.ysm.capability.ProjectileModelInfoCapability;
import com.elfmcys.ysm.capability.VehicleModelInfoCapability;
import com.elfmcys.ysm.client.event.EntityLoadEvent;
import com.elfmcys.ysm.geckolib3.core.molang.util.StringPool;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.proto.network.protocol.v0.minecraft.MinecraftStateV0;
import com.elfmcys.ysm.proto.network.protocol.v0.CommonV0;
import com.elfmcys.ysm.network.protocol.ModelReferenceCodec;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class MinecraftStateHandler {
    private MinecraftStateHandler() {
    }

    public static MinecraftStateV0.ProjectileModelState projectile(
            int entityId, ProjectileModelInfoCapability capability) {
        var message = MinecraftStateV0.ProjectileModelState.newInstance()
                .setEntity(CommonV0.EntityRef.newInstance().setEntityId(entityId));
        setState(message.getMutableModel(), message::addMolangVariables,
                capability.getOwnerModelHash(), capability.getMolangVarsServerBound());
        return message;
    }

    public static MinecraftStateV0.VehicleModelState vehicle(
            int entityId, VehicleModelInfoCapability capability) {
        var message = MinecraftStateV0.VehicleModelState.newInstance()
                .setEntity(CommonV0.EntityRef.newInstance().setEntityId(entityId));
        setState(message.getMutableModel(), message::addMolangVariables,
                capability.getOwnerModelHash(), capability.getMolangVarsServerBound());
        return message;
    }

    public static void handleProjectile(MinecraftStateV0.ProjectileModelState message,
                                        Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        if (!message.hasEntity() || !message.hasModel()
                || !ModelReferenceCodec.valid(message.getModel())) {
            context.setPacketHandled(true);
            return;
        }
        var modelHash = ModelReferenceCodec.read(message.getModel());
        var variables = clientVariables(message.getMolangVariables());
        context.enqueueWork(() -> EntityLoadEvent.executeOnEntity(message.getEntity().getEntityId(),
                entity -> applyProjectile(entity, modelHash, variables)));
        context.setPacketHandled(true);
    }

    public static void handleVehicle(MinecraftStateV0.VehicleModelState message,
                                     Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        if (!message.hasEntity() || !message.hasModel()
                || !ModelReferenceCodec.valid(message.getModel())) {
            context.setPacketHandled(true);
            return;
        }
        var modelHash = ModelReferenceCodec.read(message.getModel());
        var variables = clientVariables(message.getMolangVariables());
        context.enqueueWork(() -> EntityLoadEvent.executeOnEntity(message.getEntity().getEntityId(),
                entity -> applyVehicle(entity, modelHash, variables)));
        context.setPacketHandled(true);
    }

    private static void setState(CommonV0.ModelReference target,
                                 java.util.function.Consumer<CommonV0.MolangVariable> variableConsumer,
                                 Hash256 modelHash,
                                 Object2FloatOpenHashMap<String> variables) {
        ModelReferenceCodec.write(target, modelHash, null);
        variables.object2FloatEntrySet().fastForEach(entry -> variableConsumer.accept(
                CommonV0.MolangVariable.newInstance()
                        .setName(entry.getKey())
                        .setValue(entry.getFloatValue())));
    }

    private static Int2FloatOpenHashMap clientVariables(
            Iterable<CommonV0.MolangVariable> variables) {
        var result = new Int2FloatOpenHashMap();
        for (var variable : variables) {
            result.put(StringPool.computeIfAbsent(variable.getName()), variable.getValue());
        }
        return result;
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("DataFlowIssue")
    private static void applyProjectile(Entity entity, Hash256 modelHash, Int2FloatOpenHashMap variables) {
        entity.getCapability(ClientLazyCapabilityProvider.CAP).ifPresent(capability -> {
            var animatable = capability.getProjectileAnimatableCapabilityProvider().initialize();
            animatable.init(modelHash);
            animatable.initRoamingVars(variables);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyVehicle(Entity entity, Hash256 modelHash, Int2FloatOpenHashMap variables) {
        entity.getCapability(ClientLazyCapabilityProvider.CAP).ifPresent(capability -> {
            var animatable = capability.getVehicleAnimatableCapabilityProvider().initialize();
            animatable.init(modelHash);
            animatable.initRoamingVars(variables);
        });
    }
}
