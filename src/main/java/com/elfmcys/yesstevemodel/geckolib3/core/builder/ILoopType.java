package com.elfmcys.yesstevemodel.geckolib3.core.builder;

public interface ILoopType {
    /**
     * 是否在动画结束后重复
     *
     * @return 是否在动画结束后重复
     */
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
