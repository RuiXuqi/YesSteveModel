package com.elfmcys.ysm.client.compat;

import lain.mods.cos.api.CosArmorAPI;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.Optional;

public class CosmeticArmorCompat {
    private static final String MOD_ID = "cosmeticarmorreworked";
    private static boolean INSTALLED;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static Optional<ItemStack> getSkinArmorItem(Player player, EquipmentSlot slot) {
        if (!slot.isArmor()) {
            return Optional.empty();
        }

        var cosInventory = CosArmorAPI.getCAStacksClient(player.getUUID());
        if (cosInventory.isSkinArmor(slot.getIndex())) {
            return Optional.of(ItemStack.EMPTY);
        }

        var skinArmor = cosInventory.getStackInSlot(slot.getIndex());
        return skinArmor.isEmpty() ? Optional.empty() : Optional.of(skinArmor);
    }
}
