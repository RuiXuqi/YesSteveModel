package com.elfmcys.yesstevemodel.client.compat;

import lain.mods.cos.api.CosArmorAPI;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public class CosmeticArmorCompat {
    private static final String MOD_ID = "cosmeticarmorreworked";
    private static boolean INSTALLED;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static ItemStack getSkinArmorItem(AbstractClientPlayer player, EquipmentSlot slot) {
        if (!slot.isArmor()) {
            return ItemStack.EMPTY;
        }

        var cosInventory = CosArmorAPI.getCAStacksClient(player.getUUID());
        if (!cosInventory.isSkinArmor(slot.getIndex())) {
            return ItemStack.EMPTY;
        }

        return cosInventory.getStackInSlot(slot.getIndex());
    }
}
