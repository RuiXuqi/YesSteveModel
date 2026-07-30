package com.elfmcys.ysm.model.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SharedCachePathsTest {
    @TempDir
    Path temp;

    @Test
    void userDefaultIsIsolatedUnderTheUnstableNamespace() {
        var previous = System.getProperty("user.home");
        try {
            System.setProperty("user.home", temp.toString());
            var paths = SharedCachePaths.userDefault();

            assertEquals(temp.resolve(".ysm/unstable").toAbsolutePath().normalize(), paths.root());
            assertEquals(paths.root().resolve("remote/v0/models"), paths.remoteModels());
            assertEquals(paths.root().resolve("remote/v0/chunks"), paths.remoteChunks());
        } finally {
            if (previous == null) {
                System.clearProperty("user.home");
            } else {
                System.setProperty("user.home", previous);
            }
        }
    }
}
