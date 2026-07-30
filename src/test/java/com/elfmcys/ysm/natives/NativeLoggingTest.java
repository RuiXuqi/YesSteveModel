package com.elfmcys.ysm.natives;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeLoggingTest {
    @Test
    void mapsStandardLog4jLevels() {
        assertEquals(0, NativeLogging.mapLevel(Level.ALL));
        assertEquals(1, NativeLogging.mapLevel(Level.TRACE));
        assertEquals(2, NativeLogging.mapLevel(Level.DEBUG));
        assertEquals(3, NativeLogging.mapLevel(Level.INFO));
        assertEquals(4, NativeLogging.mapLevel(Level.WARN));
        assertEquals(5, NativeLogging.mapLevel(Level.ERROR));
        assertEquals(6, NativeLogging.mapLevel(Level.FATAL));
        assertEquals(7, NativeLogging.mapLevel(Level.OFF));
    }

    @Test
    void mapsCustomLevelToNearestSupportedThreshold() {
        var custom = Level.forName("YSM_NATIVE_LOGGING_TEST", 350);

        assertEquals(4, NativeLogging.mapLevel(custom));
    }
}
