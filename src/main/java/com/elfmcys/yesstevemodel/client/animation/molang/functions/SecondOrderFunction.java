package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.client.animation.molang.functions.physics.IPhysics;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.physics.SecondOrder;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.EntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.AnimationProcessor;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.StringUtils;

public class SecondOrderFunction extends EntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        String key = arguments.getAsString(context, 0);
        if (StringUtils.isBlank(key)) {
            return 0;
        }
        float input = arguments.getAsFloat(context, 1);

        int size = arguments.size();
        float frequency = 1, coefficient = 1, response = 1;
        if (size >= 3) {
            frequency = arguments.getAsFloat(context, 2);
        }
        if (size >= 4) {
            coefficient = arguments.getAsFloat(context, 3);
        }
        if (size >= 5) {
            response = arguments.getAsFloat(context, 4);
        }

        AnimationProcessor<?> processor = context.entity().animatableEntity().getAnimationProcessor();
        IPhysics physicsValue = processor.getPhysicsValue(key);
        if (physicsValue == null) {
            SecondOrder secondOrder = new SecondOrder(input, frequency, coefficient, response);
            processor.putPhysicsValue(key, secondOrder);
            return input;
        }
        physicsValue.setArgs(input, frequency, coefficient, response);
        return physicsValue.getValue();
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 2;
    }
}
