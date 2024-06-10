package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.entity.LivingEntity;

public class ItemMaxDurability extends LivingEntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        int number = arguments.getAsInt(context, 0);
        LivingEntity entity = context.entity().entity();
        if (number == 0) {
            return entity.getMainHandItem().getMaxDamage();
        }
        return entity.getOffhandItem().getMaxDamage();
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
