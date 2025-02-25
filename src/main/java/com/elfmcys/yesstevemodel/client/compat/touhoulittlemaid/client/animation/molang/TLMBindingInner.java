package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.molang;

import com.elfmcys.yesstevemodel.client.animation.molang.TLMBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class TLMBindingInner {
    public static void addInnerBinding(TLMBinding binding) {
        binding.livingEntityVar("is_begging", checkMaid(EntityMaid::isBegging));
        binding.livingEntityVar("is_sitting", checkMaid(EntityMaid::isMaidInSittingPose));
        binding.livingEntityVar("has_backpack", checkMaid(EntityMaid::hasBackpack));
    }

    @NotNull
    private static IValueEvaluator<Object, IContext<LivingEntity>> checkMaid(Function<EntityMaid, Boolean> predicate) {
        return ctx -> {
            LivingEntity entity = ctx.entity();
            if (entity instanceof EntityMaid maid) {
                return predicate.apply(maid);
            }
            return false;
        };
    }
}