package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.client.animation.molang.struct.Vec3fStruct;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.EntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public abstract class BoneParamFunction extends EntityFunction {
    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }

    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        var boneName = arguments.getAsPooledString(context, 0);
        if (boneName == StringPool.EMPTY) {
            return null;
        }

        var bone = context.entity().animatableEntity().getBone(boneName);
        if (bone == null) {
            return null;
        }

        return getParam(bone);
    }

    protected abstract Vec3fStruct getParam(@NotNull IBone bone);
}
