package com.elfmcys.ysm.geckolib3.core.molang.builtin.query;

import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.function.entity.LivingEntityFunction;
import com.elfmcys.ysm.geckolib3.util.MolangUtils;
import com.elfmcys.ysm.molang.runtime.ExecutionContext;
import com.elfmcys.ysm.util.EquipmentUtil;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.registries.ForgeRegistries;

public class EquippedItemAllTags extends LivingEntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        EquipmentSlot slotType = MolangUtils.parseSlotType(context.entity(), arguments.getAsString(context, 0));
        if (slotType == null) {
            return null;
        }

        ItemStack itemStack = EquipmentUtil.getEquippedItem(context.entity().entity(), slotType);
        if(itemStack.isEmpty()) {
            return false;
        }

        for (int i = 1; i < arguments.size(); i++) {
            ResourceLocation id = arguments.getAsResourceLocation(context, i);
            if (id == null) {
                return null;
            }
            TagKey<Item> tag = ForgeRegistries.ITEMS.tags().createTagKey(id);
            if (!itemStack.is(tag)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 2;
    }
}
