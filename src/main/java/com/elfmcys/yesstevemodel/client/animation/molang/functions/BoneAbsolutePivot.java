package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.client.animation.molang.struct.Vec3fStruct;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import org.jetbrains.annotations.NotNull;

public final class BoneAbsolutePivot extends BoneParamFunction {
    @Override
    protected Vec3fStruct getParam(@NotNull IBone bone) {
        if (!bone.isTracking()) {
            bone.setTracking(true);
        }
        return new BonePivotStruct(bone);
    }

    private static final class BonePivotStruct extends Vec3fStruct {
        private final IBone bone;

        public BonePivotStruct(IBone bone) {
            this.bone = bone;
        }

        @Override
        protected float getX() {
            return bone.getAbsolutePivotX();
        }

        @Override
        protected float getY() {
            return bone.getAbsolutePivotY();
        }

        @Override
        protected float getZ() {
            return bone.getAbsolutePivotZ();
        }
    }
}
