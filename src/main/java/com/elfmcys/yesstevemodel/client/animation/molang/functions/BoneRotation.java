package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.client.animation.molang.struct.Vec3fStruct;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import org.jetbrains.annotations.NotNull;

public final class BoneRotation extends BoneParamFunction {
    @Override
    protected Vec3fStruct getParam(@NotNull IBone bone) {
        return new BoneRotationStruct(bone);
    }

    private static final class BoneRotationStruct extends Vec3fStruct {
        private final IBone bone;

        public BoneRotationStruct(IBone bone) {
            this.bone = bone;
        }

        @Override
        protected float getX() {
            return (float) Math.toDegrees(bone.getRotationX());
        }

        @Override
        protected float getY() {
            return (float) Math.toDegrees(bone.getRotationY());
        }

        @Override
        protected float getZ() {
            return (float) Math.toDegrees(bone.getRotationZ());
        }
    }
}
