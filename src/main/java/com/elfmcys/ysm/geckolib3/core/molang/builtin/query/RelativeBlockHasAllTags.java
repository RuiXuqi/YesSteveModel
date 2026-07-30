package com.elfmcys.ysm.geckolib3.core.molang.builtin.query;

import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.function.entity.EntityFunction;
import com.elfmcys.ysm.geckolib3.util.MolangUtils;
import com.elfmcys.ysm.molang.runtime.ExecutionContext;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.registries.ForgeRegistries;

public class RelativeBlockHasAllTags extends EntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> ctx, ArgumentCollection arguments) {
        var block = MolangUtils.getRelativeBlock(ctx, arguments);
        if (block == null) {
            return null;
        }
        for (int i = 3; i < arguments.size(); i++) {
            ResourceLocation tagId = arguments.getAsResourceLocation(ctx, i);
            if (tagId == null) {
                return null;
            }

            TagKey<Block> tag = ForgeRegistries.BLOCKS.tags().createTagKey(tagId);
            if (!block.is(tag)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 4;
    }
}
