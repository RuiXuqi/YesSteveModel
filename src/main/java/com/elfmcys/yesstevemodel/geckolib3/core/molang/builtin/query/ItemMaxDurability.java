package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query;

import com.elfmcys.yesstevemodel.client.animation.condition.ConditionArmor;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.google.common.collect.Maps;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class ItemMaxDurability extends LivingEntityFunction {
    private static final Map<String, EquipmentSlot> MAPS = Maps.newHashMap();

    @Override
    protected Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        String slotName = arguments.getAsString(context, 0);
        EquipmentSlot equipmentSlot;
        if (MAPS.containsKey(slotName)) {
            equipmentSlot = MAPS.get(slotName);
        } else {
            equipmentSlot = ConditionArmor.getType(slotName);
            if (equipmentSlot == null) {
                return 0;
            } else {
                MAPS.put(slotName, equipmentSlot);
            }
        }
        LivingEntity entity = context.entity().entity();
        ItemStack itemBySlot = entity.getItemBySlot(equipmentSlot);
        return itemBySlot.getMaxDamage();
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
