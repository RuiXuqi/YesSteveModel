package com.elfmcys.ysm.client.compat.jade;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.ysm.capability.VehicleAnimatableCapabilityProvider;
import com.elfmcys.ysm.util.ModelIdUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
@SuppressWarnings("unused")
public class JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration reg) {
        reg.registerEntityComponent(new YSMProvider(), Entity.class);
    }

    private static class YSMProvider implements IEntityComponentProvider {
        @SuppressWarnings("removal")
        private static final ResourceLocation UID = new ResourceLocation(YesSteveModel.MOD_ID, "model_info");

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor entityAccessor, IPluginConfig config) {
            if (entityAccessor.getEntity() instanceof Player player) {
                player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                    if (cap.isInitializedAndEnabled()) {
                        tooltip.add(Component.translatable("top.yes_steve_model.model_info.id")
                                .append(cap.getModelContainer().getDisplayName(ModelIdUtil.getFileNameFromPath(cap.getModelId()))));
                    }
                });
            } else {
                entityAccessor.getEntity().getCapability(VehicleAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                    if (cap.isInitialized() && cap.isModelPresent()) {
                        tooltip.add(Component.translatable("top.yes_steve_model.model_info.id")
                                .append(cap.getModelContainer().getDisplayName(ModelIdUtil.getFileNameFromPath(cap.getModelId()))));
                    }
                });
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}