package com.elfmcys.ysm.geckolib3.core.molang.function.blocks;

import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.function.ContextFunction;
import net.minecraft.world.level.block.Block;

public abstract class BlockFunction extends ContextFunction<Block> {
    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Block;
    }
}
