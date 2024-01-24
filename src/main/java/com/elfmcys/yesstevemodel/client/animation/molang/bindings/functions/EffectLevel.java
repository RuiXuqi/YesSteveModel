package com.elfmcys.yesstevemodel.client.animation.molang.bindings.functions;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.ContextFunction;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.mixin.client.ArrowEntityAccessor;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class EffectLevel extends ContextFunction<Entity> {
    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }

    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        ResourceLocation effectId = MolangUtils.parseResourceLocation(context.entity(), arguments.getAsString(context, 0));
        if (effectId == null) {
            return null;
        }

        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        if (effect == null) {
            context.entity().debugPrint("Unknown effect id: %s", effectId);
            return 0;
        }

        if (context.entity().entity() instanceof Arrow) {
            for (MobEffectInstance instance : ((ArrowEntityAccessor) context.entity().entity()).getEffects()) {
                if (instance.getEffect() == effect) {
                    return instance.getAmplifier() + 1;
                }
            }
        } else if (context.entity().entity() instanceof LivingEntity) {
            MobEffectInstance instance = ((LivingEntity) context.entity().entity()).getEffect(effect);
            if (instance != null) {
                return instance.getAmplifier() + 1;
            }
        } else {
            return null;
        }

        return 0;
    }
}
