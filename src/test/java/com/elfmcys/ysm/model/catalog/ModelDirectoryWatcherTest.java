package com.elfmcys.ysm.model.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelDirectoryWatcherTest {
    @TempDir
    Path temp;

    @Test
    void observesChangesBelowRegisteredRoots() throws Exception {
        var root = Files.createDirectories(temp.resolve("custom/nested"));
        var events = new LinkedBlockingQueue<SourceChangeSet>();
        try (var watcher = new ModelDirectoryWatcher(List.of(
                new ModelCatalogSource(CatalogRootKind.CUSTOM, temp.resolve("custom"), true)),
                events::add)) {
            var model = root.resolve("model.mxc").toAbsolutePath().normalize();
            Files.writeString(model, "changed");

            var event = events.poll(5, TimeUnit.SECONDS);
            assertNotNull(event);
            assertTrue(event.overflow() || event.paths().contains(model));
        }
    }
}
