package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.EntityFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class DumpRelativeBlock extends EntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> ctx, ArgumentCollection arguments) {
        if (!ctx.entity().isDebugEnabled()) {
            return null;
        }

        Entity entity = ctx.entity().entity();

        int offsetX = arguments.getAsInt(ctx, 0);
        int offsetY = arguments.getAsInt(ctx, 1);
        int offsetZ = arguments.getAsInt(ctx, 2);
        if (Math.abs(offsetX) > 8 || Math.abs(offsetY) > 8 || Math.abs(offsetZ) > 8) {
            ctx.entity().debugPrint("Argument out of range");
            return null;
        }

        BlockState block = ctx.entity().entity().level().getBlockState(entity.blockPosition());
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        if (blockId == null) {
            return null;
        }
        ctx.entity().debugPrint("Display: '%s'", block.getBlock().getName().getString(99));
        ctx.entity().debugPrint("Name: '%s'", blockId);

        ForgeRegistries.BLOCKS.tags().getReverseTag(block.getBlock()).ifPresent(tags -> {
            tags.getTagKeys().forEach(key -> {
                ctx.entity().debugPrint("Tag: '%s'", key);
            });
        });

        return null;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 3;
    }
}
