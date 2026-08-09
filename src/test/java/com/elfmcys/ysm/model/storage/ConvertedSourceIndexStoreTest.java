package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.model.catalog.CatalogRootIdentity;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.catalog.ModelSourceKey;
import com.elfmcys.ysm.model.catalog.ModelSourceKind;
import com.elfmcys.ysm.model.catalog.SourceStamp;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.ModelPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvertedSourceIndexStoreTest {
    @TempDir
    Path temp;

    @Test
    void exactGateRoundTripsAndMalformedEntryIsOnlyAMiss() throws Exception {
        var paths = new SharedCachePaths(temp.resolve("cache"));
        var profile = new ConversionProfileId(hash(1));
        var store = new ConvertedSourceIndexStore(paths, new AtomicSharedCache(paths), profile);
        var root = new CatalogRootIdentity(CatalogRootKind.CUSTOM,
                temp.resolve("models").toAbsolutePath(), "root-key");
        var key = new ModelSourceKey(root, new ModelPath("group/model"),
                ModelSourceKind.CURRENT_RAW_DIRECTORY);
        var value = new ConvertedSourceIndex(key,
                new SourceStamp.RawDirectory(hash(2), 3, 42), profile, hash(3), hash(4));

        store.write(value);
        assertEquals(value, store.read(key).orElseThrow());

        Files.write(store.path(key), new byte[]{1, 2, 3});
        assertTrue(store.read(key).isEmpty());
    }

    private static Hash256 hash(int seed) {
        var bytes = new byte[Hash256.SIZE];
        bytes[0] = (byte) seed;
        return new Hash256(bytes);
    }
}
