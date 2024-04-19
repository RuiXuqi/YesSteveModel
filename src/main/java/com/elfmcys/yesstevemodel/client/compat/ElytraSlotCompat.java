package com.elfmcys.yesstevemodel.client.compat;

import com.illusivesoulworks.elytraslot.platform.Services;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public class ElytraSlotCompat {
    private static final String MOD_ID = "elytraslot";
    private static boolean INSTALLED;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static ItemStack getEquippedElytraItem(LivingEntity entity) {
        return Services.ELYTRA.getEquipped(entity);
    }
}
