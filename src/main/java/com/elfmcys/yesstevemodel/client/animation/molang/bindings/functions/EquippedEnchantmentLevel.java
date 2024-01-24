package com.elfmcys.yesstevemodel.client.animation.molang.bindings.functions;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class EquippedEnchantmentLevel extends LivingEntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        EquipmentSlot slotType = MolangUtils.parseSlotType(context.entity(), arguments.getAsString(context, 0));
        if (slotType == null) {
            return null;
        }

        ResourceLocation id = MolangUtils.parseResourceLocation(context.entity(), arguments.getAsString(context, 1));
        if (id == null) {
            return null;
        }

        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
        if (enchantment == null) {
            return 0;
        }

        ItemStack itemStack = context.entity().entity().getItemBySlot(slotType);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, itemStack);
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 2;
    }
}
