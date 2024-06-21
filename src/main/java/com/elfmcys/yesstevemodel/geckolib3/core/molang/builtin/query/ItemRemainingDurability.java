package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.google.common.collect.Maps;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class ItemRemainingDurability extends LivingEntityFunction {
    private static final Map<String, EquipmentSlot> MAPS = Maps.newHashMap();

    @Override
    protected Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        EquipmentSlot equipmentSlot = MolangUtils.parseSlotType(context.entity(), arguments.getAsString(context, 0));
        LivingEntity entity = context.entity().entity();
        ItemStack itemBySlot = entity.getItemBySlot(equipmentSlot);
        return itemBySlot.getMaxDamage() - itemBySlot.getDamageValue();
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
