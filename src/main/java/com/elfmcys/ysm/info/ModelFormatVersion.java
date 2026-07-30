package com.elfmcys.ysm.info;

import com.elfmcys.ysm.geckolib3.core.builder.Animation;
import com.elfmcys.ysm.geckolib3.core.event.predicate.AnimationEvent;

public class ModelFormatVersion {
    public static final int LEGACY = 0;
    public static final int VER_2_4_2_SNAP21 = 19;
    public static final int PLAIN = 0XFFFF;

    // 未加密模型和 19 序号（包含）之后的手部动画都直接让动画文件决定播放类型
    public static boolean shouldIgnoreCodedLoopTypeForHandAnim(AnimationEvent<?> event, String animationName, int formatVer) {
        if (formatVer >= VER_2_4_2_SNAP21) {
            return true;
        }
        Animation animation = event.getAnimatableEntity().getAnimation(animationName);
        if (animation == null) {
            // 动画文件不存在，其实返回 false 或者 true 都无所谓
            return false;
        }
        // 如果是从默认模型复制过来的动画，则使用非硬编码的动画播放类型
        return animation.isCopiedFromDefaultModel;
    }
}
