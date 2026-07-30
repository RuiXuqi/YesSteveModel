package com.elfmcys.ysm.client.animation.molang.functions;

import com.elfmcys.ysm.client.particle.ParticleSpawner;
import com.elfmcys.ysm.geckolib3.core.molang.context.IContext;
import com.elfmcys.ysm.geckolib3.core.molang.function.entity.EntityFunction;
import com.elfmcys.ysm.molang.runtime.ExecutionContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.ExecutionException;

public class ParticleFunction extends EntityFunction {
    private final boolean absPos;

    public ParticleFunction(boolean absPos) {
        this.absPos = absPos;
    }

    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        if (!context.entity().allowEmitting() || context.entity().animatableEntity().isFakePlayer()) {
            return null;
        }
        try {
            return ParticleSpawner.evalSpawnParticle(context, arguments, this.absPos);
        } catch (ExecutionException | CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 1;
    }
}
