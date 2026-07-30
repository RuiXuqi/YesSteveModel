package com.elfmcys.ysm.natives;

import com.elfmcys.ysm.testutil.NativeLibraryExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@EnabledIfEnvironmentVariable(named = "YSM_NATIVE_PATH", matches = ".+")
@ExtendWith(NativeLibraryExtension.class)
class NativeLoggingIntegrationTest {
    @Test
    void synchronizesCurrentLog4jLevelWithNativeLibrary() {
        assertDoesNotThrow(NativeLogging::syncLevel);
    }
}
