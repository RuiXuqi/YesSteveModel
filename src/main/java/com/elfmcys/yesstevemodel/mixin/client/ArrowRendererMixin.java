package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.api.IArrowExtraInfo;
import com.elfmcys.yesstevemodel.client.event.RegisterEntityRenderersEvent;
import com.elfmcys.yesstevemodel.client.instance.CustomArrowInstance;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArrowRenderer.class)
public class ArrowRendererMixin {
    @Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/world/entity/projectile/AbstractArrow;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", cancellable = true)
    public void render(AbstractArrow pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, CallbackInfo callback) {
        if (!GeneralConfig.DISABLE_ARROWS_MODEL.get() && pEntity instanceof IArrowExtraInfo) {
            CustomArrowInstance instance = (CustomArrowInstance) ((IArrowExtraInfo) pEntity).getGeoInstance();
            if (instance != null) {
                RegisterEntityRenderersEvent.getArrowRenderer().render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
                callback.cancel();
            }
        }
    }
}
