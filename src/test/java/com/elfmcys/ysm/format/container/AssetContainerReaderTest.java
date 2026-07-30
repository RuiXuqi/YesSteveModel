package com.elfmcys.ysm.format.container;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AssetContainerReaderTest {
    @Test
    void rejectsPreambleWithInvalidMagicBeforeParsingMetadata() {
        var invalidPreamble = new byte[AssetContainerConstant.HEAD.length + 1];

        assertThrows(IOException.class, () -> AssetContainerReader.readPreamble(invalidPreamble));
    }
}
