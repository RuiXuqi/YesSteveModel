package com.elfmcys.yesstevemodel.client.renderer.replace;

import com.elfmcys.yesstevemodel.capability.VehicleAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.event.RegisterEntityRenderersEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

public class EntityRendererReplace {
    public static boolean renderInMixin(Entity entity, float yaw, float partialTick, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight) {
        return entity.getCapability(VehicleAnimatableCapabilityProvider.CAP).map(cap -> {
            if (cap.isInitialized() && cap.isModelPresent()) {
                RegisterEntityRenderersEvent.getVehicleRenderer().render(cap, yaw, partialTick, poseStack, bufferSource, packedLight);
                return false;
            }
            return true;
        }).orElse(true);
    }
}
