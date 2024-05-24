package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;


public class DumpEquippedItem extends LivingEntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        if (!context.entity().isDebugEnabled()) {
            return null;
        }

        EquipmentSlot slotType = MolangUtils.parseSlotType(context.entity(), arguments.getAsString(context, 0));
        if (slotType == null) {
            return null;
        }

        ItemStack itemStack = context.entity().entity().getItemBySlot(slotType);
        if (itemStack.isEmpty()) {
            return null;
        }

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        if (id == null) {
            return null;
        }
        context.entity().debugPrint("Display: '%s'", itemStack.getItem().getName(itemStack).getString(99));
        context.entity().debugPrint("Name: '%s'", id);

        ForgeRegistries.ITEMS.tags().getReverseTag(itemStack.getItem()).ifPresent(tags -> {
            tags.getTagKeys().forEach(key -> {
                context.entity().debugPrint("Tag: '%s'", key);
            });
        });

        for (Tag nbt : itemStack.getEnchantmentTags()) {
            if (nbt instanceof CompoundTag) {
                CompoundTag compoundnbt = (CompoundTag) nbt;
                ResourceLocation enchantmentId = ResourceLocation.tryParse(compoundnbt.getString("id"));
                if (enchantmentId == null) {
                    continue;
                }
                Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                if (enchantment == null) {
                    continue;
                }
                int level = compoundnbt.getInt("lvl");
                context.entity().debugPrint("Enchantment: display='%s' name='%s'",
                        enchantment.getFullname(level).getString(99), enchantmentId);
            }
        }

        return null;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
