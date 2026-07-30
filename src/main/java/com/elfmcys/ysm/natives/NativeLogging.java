package com.elfmcys.ysm.natives;

import com.elfmcys.ysm.YesSteveModel;
import org.apache.logging.log4j.Level;

public final class NativeLogging {
    private static final int LEVEL_ALL = 0;
    private static final int LEVEL_TRACE = 1;
    private static final int LEVEL_DEBUG = 2;
    private static final int LEVEL_INFO = 3;
    private static final int LEVEL_WARNING = 4;
    private static final int LEVEL_ERROR = 5;
    private static final int LEVEL_FATAL = 6;
    private static final int LEVEL_OFF = 7;

    private NativeLogging() {
    }

    public static void syncLevel() {
        var level = YesSteveModel.LOGGER.getLevel();
        if (!nSetLevel(mapLevel(level))) {
            throw new IllegalStateException("Native logging rejected log4j level " + level);
        }
    }

    static int mapLevel(Level level) {
        if (level == Level.ALL) {
            return LEVEL_ALL;
        }
        if (level == Level.OFF) {
            return LEVEL_OFF;
        }

        int value = level.intLevel();
        if (value >= Level.TRACE.intLevel()) {
            return LEVEL_TRACE;
        }
        if (value >= Level.DEBUG.intLevel()) {
            return LEVEL_DEBUG;
        }
        if (value >= Level.INFO.intLevel()) {
            return LEVEL_INFO;
        }
        if (value >= Level.WARN.intLevel()) {
            return LEVEL_WARNING;
        }
        if (value >= Level.ERROR.intLevel()) {
            return LEVEL_ERROR;
        }
        if (value >= Level.FATAL.intLevel()) {
            return LEVEL_FATAL;
        }
        return LEVEL_OFF;
    }

    private static native boolean nSetLevel(int level);
}
