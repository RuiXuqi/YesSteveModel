package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.client.animation.molang.struct.Vec3fStruct;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import org.jetbrains.annotations.NotNull;

public final class BoneScale extends BoneParamFunction {
    @Override
    protected Vec3fStruct getParam(@NotNull IBone bone) {
        return new BoneScaleStruct(bone);
    }

    private static final class BoneScaleStruct extends Vec3fStruct {
        private final IBone bone;

        public BoneScaleStruct(IBone bone) {
            this.bone = bone;
        }

        @Override
        protected float getX() {
            return bone.getScaleX();
        }

        @Override
        protected float getY() {
            return bone.getScaleY();
        }

        @Override
        protected float getZ() {
            return bone.getScaleZ();
        }
    }
}
