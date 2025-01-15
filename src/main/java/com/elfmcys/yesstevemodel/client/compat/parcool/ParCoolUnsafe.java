package com.elfmcys.yesstevemodel.client.compat.parcool;

import com.alrex.parcool.client.animation.impl.WallJumpAnimator;
import com.elfmcys.yesstevemodel.util.UnsafeUtil;

public class ParCoolUnsafe {
    private static long FIELD_OFFSET = -1;

    public static void initFiledOffset() {
        try {
            if (ParCoolCompat.isVersion3310()) {
                FIELD_OFFSET = UnsafeUtil.getUnsafe().objectFieldOffset(WallJumpAnimator.class.getDeclaredField("wallRightSide"));
            } else {
                FIELD_OFFSET = UnsafeUtil.getUnsafe().objectFieldOffset(WallJumpAnimator.class.getDeclaredField("swingRightArm"));
            }
        } catch (NoSuchFieldException e) {
            e.fillInStackTrace();
        }
    }

    public static boolean isSwingRightArm(WallJumpAnimator animator) {
        return UnsafeUtil.getUnsafe().getBoolean(animator, FIELD_OFFSET);
    }
}
