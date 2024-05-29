package com.elfmcys.yesstevemodel.client.compat.slashblade;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public class SlashBladeCompat {
    private static final String SLASH_BLADE_ID = "slashblade";

    public static boolean isSlashBladeLoaded() {
        return ModList.get().isLoaded(SLASH_BLADE_ID);
    }

    public static boolean isSlashBladeItem(ItemStack stack) {
        return isSlashBladeLoaded() && stack.getItem() instanceof ItemSlashBlade;
    }
}
