package com.elfmcys.ysm.natives;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NativeRuntimeTest {
    @Test
    void mapsStandardLog4jLevels() {
        assertEquals(0, NativeRuntime.JavaConfig.fromLog4j(Level.ALL).logLevel());
        assertEquals(1, NativeRuntime.JavaConfig.fromLog4j(Level.TRACE).logLevel());
        assertEquals(2, NativeRuntime.JavaConfig.fromLog4j(Level.DEBUG).logLevel());
        assertEquals(3, NativeRuntime.JavaConfig.fromLog4j(Level.INFO).logLevel());
        assertEquals(4, NativeRuntime.JavaConfig.fromLog4j(Level.WARN).logLevel());
        assertEquals(5, NativeRuntime.JavaConfig.fromLog4j(Level.ERROR).logLevel());
        assertEquals(6, NativeRuntime.JavaConfig.fromLog4j(Level.FATAL).logLevel());
        assertEquals(7, NativeRuntime.JavaConfig.fromLog4j(Level.OFF).logLevel());
    }

    @Test
    void mapsCustomLevelToNearestSupportedThreshold() {
        var custom = Level.forName("YSM_NATIVE_RUNTIME_TEST", 350);

        assertEquals(4, NativeRuntime.JavaConfig.fromLog4j(custom).logLevel());
    }

    @Test
    void packsVersionAndLogLevelIntoJavaConfig() {
        assertEquals(1L | (3L << 8), NativeRuntime.JavaConfig.fromLog4j(Level.INFO).pack());
    }

    @Test
    void rejectsInvalidLogLevel() {
        assertThrows(IllegalArgumentException.class, () -> new NativeRuntime.JavaConfig(8));
    }
}
