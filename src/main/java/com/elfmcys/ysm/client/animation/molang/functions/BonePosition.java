package com.elfmcys.ysm.client.animation.molang.functions;

import com.elfmcys.ysm.client.animation.molang.struct.Vec3fStruct;
import com.elfmcys.ysm.geckolib3.core.processor.BoneView;
import org.jetbrains.annotations.NotNull;

public final class BonePosition extends BoneParamFunction {
    @Override
    protected Vec3fStruct getParam(@NotNull BoneView bone) {
        return new BonePositionStruct(bone);
    }

    private static final class BonePositionStruct extends Vec3fStruct {
        private final BoneView bone;

        public BonePositionStruct(BoneView bone) {
            this.bone = bone;
        }

        @Override
        protected float getX() {
            return bone.getPositionX();
        }

        @Override
        protected float getY() {
            return bone.getPositionY();
        }

        @Override
        protected float getZ() {
            return bone.getPositionZ();
        }
    }
}
