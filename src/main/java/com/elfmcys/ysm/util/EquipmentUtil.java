package com.elfmcys.ysm.util;

import com.elfmcys.ysm.client.compat.CosmeticArmorCompat;
import com.elfmcys.ysm.client.compat.ElytraSlotCompat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EquipmentUtil {
    public static ItemStack getEquippedItem(LivingEntity entity, EquipmentSlot slot) {
        if (slot.isArmor() && entity instanceof Player player && CosmeticArmorCompat.isInstalled()) {
            var stack = CosmeticArmorCompat.getSkinArmorItem(player, slot);
            if (stack.isPresent()) {
                return stack.get();
            }
        }
        var rawItem = entity.getItemBySlot(slot);
        return rawItem;
    }

    public static ItemStack getEquippedElytraItem(LivingEntity entity) {
        // 时装盔甲
        if (entity instanceof Player player && CosmeticArmorCompat.isInstalled()) {
            var stack = CosmeticArmorCompat.getSkinArmorItem(player, EquipmentSlot.CHEST);
            if (stack.isPresent() && stack.get().getItem() == Items.ELYTRA) {
                return stack.get();
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
