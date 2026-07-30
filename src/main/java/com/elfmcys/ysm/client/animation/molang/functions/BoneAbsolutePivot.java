package com.elfmcys.ysm.client.animation.molang.functions;

import com.elfmcys.ysm.client.animation.molang.struct.Vec3fStruct;
import com.elfmcys.ysm.geckolib3.core.processor.BoneView;
import org.jetbrains.annotations.NotNull;

public final class BoneAbsolutePivot extends BoneParamFunction {
    @Override
    protected Vec3fStruct getParam(@NotNull BoneView bone) {
        /*
        if (!bone.isTracking()) {
            bone.setTracking(true);
        }
        return new BonePivotStruct(bone);
        */
        // TODO
        return null;
    }

    private static final class BonePivotStruct extends Vec3fStruct {
        private final BoneView bone;

        public BonePivotStruct(BoneView bone) {
            this.bone = bone;
        }

        @Override
        protected float getX() {
            // TODO
            // return bone.getAbsolutePivotX();
            return 0;
        }

        @Override
        protected float getY() {
            // TODO
            // return bone.getAbsolutePivotY();
            return 0;
        }

        @Override
        protected float getZ() {
            // TODO
            // return bone.getAbsolutePivotZ();
            return 0;
        }
    }
}
