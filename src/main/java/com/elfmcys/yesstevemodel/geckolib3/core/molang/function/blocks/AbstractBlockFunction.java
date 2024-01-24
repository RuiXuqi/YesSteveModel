package com.elfmcys.yesstevemodel.geckolib3.core.molang.function.blocks;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.ContextFunction;
import net.minecraft.world.level.block.state.BlockBehaviour;

public abstract class AbstractBlockFunction extends ContextFunction<BlockBehaviour> {
    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof BlockBehaviour;
    }
}
