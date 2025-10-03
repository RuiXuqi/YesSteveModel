package com.elfmcys.yesstevemodel.info;

public class ModelFormatVersion {
    public static final int LEGACY = 0;
    public static final int VER_2_4_2_SNAP21 = 19;
    public static final int PLAIN = 0XFFFF;

    // 未加密模型和 19 序号（包含）之后的手部动画都直接让动画文件决定播放类型
    public static boolean shouldIgnoreCodedLoopTypeForHandAnim(int formatVer) {
        return formatVer >= VER_2_4_2_SNAP21;
    }
}
