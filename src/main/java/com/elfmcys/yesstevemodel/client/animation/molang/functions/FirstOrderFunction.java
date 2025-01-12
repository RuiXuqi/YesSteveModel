package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.client.animation.molang.functions.physics.FirstOrder;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.physics.IPhysics;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.EntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.AnimationProcessor;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.StringUtils;

public class FirstOrderFunction extends EntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        String key = arguments.getAsString(context, 0);
        if (StringUtils.isBlank(key)) {
            return 0;
        }
        float input = arguments.getAsFloat(context, 1);

        int size = arguments.size();
        float response = 1;
        if (size >= 3) {
            response = arguments.getAsFloat(context, 2);
        }

        AnimationProcessor<?> processor = context.entity().animatableEntity().getAnimationProcessor();
        IPhysics physicsValue = processor.getPhysicsValue(key);
        if (physicsValue == null) {
            FirstOrder firstOrder = new FirstOrder(input, response);
            processor.putPhysicsValue(key, firstOrder);
            return input;
        }
        physicsValue.setArgs(input, response);
        return physicsValue.getValue();
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 2;
    }
}
