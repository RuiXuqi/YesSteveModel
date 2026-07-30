package com.elfmcys.ysm.geckolib3.core.molang.function.item;

import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.function.ContextFunction;
import net.minecraft.world.item.ItemStack;

public abstract class ItemStackFunction extends ContextFunction<ItemStack> {
    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof ItemStack;
    }
}
