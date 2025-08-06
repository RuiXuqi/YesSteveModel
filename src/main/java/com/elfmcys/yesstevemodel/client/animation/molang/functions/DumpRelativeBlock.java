package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.EntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class DumpRelativeBlock extends EntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> ctx, ArgumentCollection arguments) {
        if (!ctx.entity().isDebugEnabled()) {
            return null;
        }

        var block = MolangUtils.getRelativeBlock(ctx, arguments);
        if (block == null) {
            return null;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        if (blockId == null) {
            return null;
        }
        ctx.entity().debugPrint(Component.literal("Display ").append(ComponentUtils.copyOnClickText(block.getBlock().getName().getString(99))));
        ctx.entity().debugPrint(Component.literal("Name ").append(ComponentUtils.copyOnClickText(blockId.toString())));
        block.getTags().forEach(key -> {
            ctx.entity().debugPrint(Component.literal("Tag ").append(ComponentUtils.copyOnClickText(key.location().toString())));
        });

        return null;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 3;
    }
}
