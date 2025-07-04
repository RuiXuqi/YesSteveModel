package com.elfmcys.yesstevemodel.geckolib3.core.builder;

public interface ILoopType {
    @Deprecated
    boolean isRepeatingAfterEnd();

    // Native Access: 所有枚举值都有读取
    enum EDefaultLoopTypes implements ILoopType {
        /**
         * 动画播放类型
         */
        LOOP(true),
        PLAY_ONCE,
        HOLD_ON_LAST_FRAME;

        private final boolean looping;

        EDefaultLoopTypes(boolean looping) {
            this.looping = looping;
        }

        EDefaultLoopTypes() {
            this(false);
        }

        @Override
        public boolean isRepeatingAfterEnd() {
            return this.looping;
        }
    }
}
