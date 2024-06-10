package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ItemRemainingDurability extends LivingEntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        int number = arguments.getAsInt(context, 0);
        LivingEntity entity = context.entity().entity();
        if (number == 0) {
            ItemStack mainHandItem = entity.getMainHandItem();
            return mainHandItem.getMaxDamage() - mainHandItem.getDamageValue();
        }
        ItemStack offhandItem = entity.getOffhandItem();
        return offhandItem.getMaxDamage() - offhandItem.getDamageValue();
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
