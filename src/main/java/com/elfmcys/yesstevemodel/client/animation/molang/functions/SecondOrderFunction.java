package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.client.animation.molang.functions.physics.IPhysics;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.physics.SecondOrder;
import com.elfmcys.yesstevemodel.client.entity.IPhysicsEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.EntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.entity.Entity;

public class SecondOrderFunction extends EntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        int key = arguments.getAsPooledString(context, 0);
        if (key == StringPool.EMPTY) {
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

        if (context.entity().animatableEntity() instanceof IPhysicsEntity physicsEntity) {
            var manager = physicsEntity.getPhysicsManager();
            IPhysics physicsValue = manager.get(key);
            if (physicsValue == null) {
                SecondOrder secondOrder = new SecondOrder(input, frequency, coefficient, response);
                manager.put(key, secondOrder);
                return input;
            }
            physicsValue.setArgs(input, frequency, coefficient, response);
            return physicsValue.getValue();
        }

        return null;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 2;
    }
}
