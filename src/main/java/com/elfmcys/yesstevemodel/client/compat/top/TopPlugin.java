package com.elfmcys.yesstevemodel.client.compat.top;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.VehicleModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import mcjty.theoneprobe.api.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class TopPlugin implements Function<ITheOneProbe, Void> {
    @Nullable
    @Override
    public Void apply(@Nullable ITheOneProbe probe) {
        if (probe != null) {
            probe.registerEntityProvider(new YSMProvider());
        }
        return null;
    }

    private static class YSMProvider implements IProbeInfoEntityProvider {
        @SuppressWarnings("removal")
        private static final String ID = (new ResourceLocation(YesSteveModel.MOD_ID, "model_info")).toString();

        @Override
        public void addProbeEntityInfo(ProbeMode probeMode, IProbeInfo probeInfo, Player sourcePlayer, Level world, Entity entity, IProbeHitEntityData iProbeHitEntityData) {
            if (entity instanceof ServerPlayer player) {
                player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(cap -> {
                    if (cap.isMandatory() || NetworkHandler.isPlayerChannelPresent(player)) {
                        ServerModelManager.getModel(cap.getModelId()).ifPresent(m -> {
                            probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                                    .text(Component.translatable("top.yes_steve_model.model_info.id").append(
                                            StringUtils.defaultIfBlank(m.info().metadata() == null ? "" : m.info().metadata().name(), ModelIdUtil.getFileNameFromPath(cap.getModelId()))));
                        });
                    }
                });
            } else {
                entity.getCapability(VehicleModelInfoCapabilityProvider.CAP).ifPresent(cap -> {
                    if (cap.isInitialized()) {
                        ServerModelManager.getModel(cap.getOwnerModelId())
                                .filter(m -> m.vehicleModels().contains(entity.getType().builtInRegistryHolder().key().location()))
                                .ifPresent(m -> {
                                    probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                                            .text(Component.translatable("top.yes_steve_model.model_info.id").append(
                                                    StringUtils.defaultIfBlank(m.info().metadata() == null ? "" : m.info().metadata().name(), ModelIdUtil.getFileNameFromPath(cap.getOwnerModelId()))));
                                });
                    }
                });
            }
        }

        @Override
        public String getID() {
            return ID;
        }
    }
}