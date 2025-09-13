package com.elfmcys.yesstevemodel.client.renderer.projectile;

import com.elfmcys.yesstevemodel.capability.ProjectileAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.event.RegisterEntityRenderersEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.projectile.Projectile;

public class ProjectileRendererReplace {
    public static boolean renderInMixin(Projectile entity, float yaw, float partialTick, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight) {
        return entity.getCapability(ProjectileAnimatableCapabilityProvider.CAP).map(cap -> {
            if (cap.isInitialized() && cap.isModelPresent()) {
                RegisterEntityRenderersEvent.getProjectRenderer().render(cap, yaw, partialTick, poseStack, bufferSource, packedLight);
                return false;
            }
            return true;
        }).orElse(true);
    }
}
