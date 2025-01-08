package com.elfmcys.yesstevemodel.client.animation.molang.functions.physics;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.PlayerEntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.AnimationProcessor;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.client.player.AbstractClientPlayer;
import org.apache.commons.lang3.StringUtils;

public class SecondOrderFunction extends PlayerEntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<AbstractClientPlayer>> context, ArgumentCollection arguments) {
        String input = arguments.getAsString(context, 0);
        if (StringUtils.isBlank(input)) {
            return 0;
        }
        int size = arguments.size();
        float frequency = 1, coefficient = 1, response = 1;
        if (size == 2) {
            frequency = arguments.getAsFloat(context, 1);
        } else if (size == 3) {
            coefficient = arguments.getAsFloat(context, 2);
        } else if (size == 4) {
            response = arguments.getAsFloat(context, 3);
        }
        AnimationProcessor<?> processor = context.entity().animatableEntity().getAnimationProcessor();
        return processor.putIfAbsentPhysicsValue(input, frequency, coefficient, response);
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 1;
    }
}
