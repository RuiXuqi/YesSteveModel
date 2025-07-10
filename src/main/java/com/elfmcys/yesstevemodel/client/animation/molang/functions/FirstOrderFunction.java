package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.client.animation.molang.functions.physics.FirstOrder;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.physics.IPhysics;
import com.elfmcys.yesstevemodel.client.entity.IPhysicsEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.EntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.entity.Entity;

public class FirstOrderFunction extends EntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        int key = arguments.getAsPooledString(context, 0);
        if (key == StringPool.EMPTY) {
            return 0;
        }
        float input = arguments.getAsFloat(context, 1);

        int size = arguments.size();
        float response = 1;
        if (size >= 3) {
            response = arguments.getAsFloat(context, 2);
        }

        if (context.entity().animatableEntity() instanceof IPhysicsEntity physicsEntity) {
            var manager = physicsEntity.getPhysicsManager();
            IPhysics physicsValue = manager.get(key);
            if (physicsValue == null) {
                FirstOrder firstOrder = new FirstOrder(input, response);
                manager.put(key, firstOrder);
                return input;
            }
            physicsValue.setArgs(input, response, 0, 0);
            return physicsValue.getValue();
        }

        return null;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 2;
    }
}
