package com.elfmcys.ysm.model.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvertedCacheLifecycleTest {
    @TempDir
    Path temp;

    @Test
    void exclusivePruneLeaseCannotOverlapSharedReader() throws Exception {
        var paths = new SharedCachePaths(temp.resolve("cache"));
        try (var reader = new ConvertedCacheLifecycle(paths);
             var cleaner = new ConvertedCacheLifecycle(paths)) {
            reader.acquireShared();
            assertTrue(reader.holdsShared());
            assertFalse(cleaner.tryAcquireStartupExclusive());
        }

        try (var cleaner = new ConvertedCacheLifecycle(paths)) {
            assertTrue(cleaner.tryAcquireStartupExclusive());
            assertTrue(cleaner.holdsExclusive());
            cleaner.releaseExclusive();
            cleaner.acquireShared();
            assertTrue(cleaner.holdsShared());
        }
    }
}
