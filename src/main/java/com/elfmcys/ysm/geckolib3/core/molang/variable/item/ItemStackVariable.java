package com.elfmcys.ysm.geckolib3.core.molang.variable.item;

import com.elfmcys.ysm.geckolib3.core.molang.variable.IValueEvaluator;
import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.variable.LambdaVariable;
import net.minecraft.world.item.ItemStack;

public class ItemStackVariable extends LambdaVariable<ItemStack> {
    public ItemStackVariable(IValueEvaluator<?, IContext<ItemStack>> evaluator) {
        super(evaluator);
    }

    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof ItemStack;
    }
}
