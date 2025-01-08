package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.client.animation.molang.CustomMolangParser;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.physics.IPhysics;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.physics.SecondOrder;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.EntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.AnimationProcessor;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class SecondOrderFunction extends EntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        String input = arguments.getAsString(context, 0);
        if (StringUtils.isBlank(input)) {
            return 0;
        }

        int size = arguments.size();
        float frequency = 1, coefficient = 1, response = 1;
        if (size >= 2) {
            frequency = arguments.getAsFloat(context, 1);
        }
        if (size >= 3) {
            coefficient = arguments.getAsFloat(context, 2);
        }
        if (size >= 4) {
            response = arguments.getAsFloat(context, 3);
        }

        // 将所有的参数作为 key，这样相同参数的函数值可以复用，不同参数的函数值不会冲突
        Integer key = Objects.hash("SecondOrder", input);
        AnimationProcessor<?> processor = context.entity().animatableEntity().getAnimationProcessor();
        IPhysics physicsValue = processor.getPhysicsValue(key);
        if (physicsValue == null) {
            putValue(input, frequency, coefficient, response, processor, key);
            return 0;
        }
        physicsValue.setArgs(frequency, coefficient, response);
        return physicsValue.getValue();
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 1;
    }

    private void putValue(String input, float frequency, float coefficient, float response, AnimationProcessor<?> processor, Integer key) {
        try {
            IValue argument = CustomMolangParser.parseSingleExpressionUnsafe(input);
            SecondOrder secondOrder = new SecondOrder(argument, frequency, coefficient, response);
            processor.putPhysicsValue(key, secondOrder);
        } catch (ParseException e) {
            e.fillInStackTrace();
        }
    }
}
