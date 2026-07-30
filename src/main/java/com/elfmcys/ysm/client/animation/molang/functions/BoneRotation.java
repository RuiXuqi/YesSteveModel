package com.elfmcys.ysm.client.animation.molang.functions;

import com.elfmcys.ysm.client.animation.molang.struct.Vec3fStruct;
import com.elfmcys.ysm.geckolib3.core.processor.BoneView;
import org.jetbrains.annotations.NotNull;

public final class BoneRotation extends BoneParamFunction {
    @Override
    protected Vec3fStruct getParam(@NotNull BoneView bone) {
        return new BoneRotationStruct(bone);
    }

    private static final class BoneRotationStruct extends Vec3fStruct {
        private final BoneView bone;

        public BoneRotationStruct(BoneView bone) {
            this.bone = bone;
        }

        @Override
        protected float getX() {
            return -(float) Math.toDegrees(bone.getRotationX());
        }

        @Override
        protected float getY() {
            return -(float) Math.toDegrees(bone.getRotationY());
        }

        @Override
        protected float getZ() {
            return (float) Math.toDegrees(bone.getRotationZ());
        }
    }
}
