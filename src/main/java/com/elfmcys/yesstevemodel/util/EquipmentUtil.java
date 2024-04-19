package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.client.compat.CosmeticArmorCompat;
import com.elfmcys.yesstevemodel.client.compat.ElytraSlotCompat;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EquipmentUtil {
    public static ItemStack getEquippedItem(LivingEntity entity, EquipmentSlot slot) {
        if (slot.isArmor() && entity instanceof AbstractClientPlayer player && CosmeticArmorCompat.isInstalled()) {
            var stack = CosmeticArmorCompat.getSkinArmorItem(player, slot);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return entity.getItemBySlot(slot);
    }

    public static ItemStack getEquippedElytraItem(LivingEntity entity) {
        // 时装盔甲
        if (entity instanceof AbstractClientPlayer player && CosmeticArmorCompat.isInstalled()) {
            var stack = CosmeticArmorCompat.getSkinArmorItem(player, EquipmentSlot.CHEST);
            if (stack.getItem() == Items.ELYTRA) {
                return stack;
            }
        }
        // 鞘翅插槽
        if (ElytraSlotCompat.isInstalled()) {
            var stack = ElytraSlotCompat.getEquippedElytraItem(entity);
            if (stack.getItem() == Items.ELYTRA) {
                return stack;
            }
        }
        // 原版鞘翅
        var stack = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (stack.getItem() == Items.ELYTRA) {
            return stack;
        }
        return ItemStack.EMPTY;
    }
}
