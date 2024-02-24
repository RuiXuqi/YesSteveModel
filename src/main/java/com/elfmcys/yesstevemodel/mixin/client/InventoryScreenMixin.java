package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.model.CustomPlayerModel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    @Inject(at = @At("HEAD"), method = "renderEntityInInventoryFollowsAngle(Lnet/minecraft/client/gui/GuiGraphics;IIIFFLnet/minecraft/world/entity/LivingEntity;)V", remap = false)
    private static void beforeRenderEntityInInventoryFollowsAngle(GuiGraphics pGuiGraphics, int pX, int pY, int pScale, float angleXComponent, float angleYComponent, LivingEntity pEntity, CallbackInfo ci) {
        CustomPlayerModel.setRenderingEntitiesInInventory(true);
    }

    @Inject(at = @At("TAIL"), method = "renderEntityInInventoryFollowsAngle(Lnet/minecraft/client/gui/GuiGraphics;IIIFFLnet/minecraft/world/entity/LivingEntity;)V", remap = false)
    private static void afterRenderEntityInInventoryFollowsAngle(GuiGraphics pGuiGraphics, int pX, int pY, int pScale, float angleXComponent, float angleYComponent, LivingEntity pEntity, CallbackInfo ci) {
        CustomPlayerModel.setRenderingEntitiesInInventory(false);
    }
}
