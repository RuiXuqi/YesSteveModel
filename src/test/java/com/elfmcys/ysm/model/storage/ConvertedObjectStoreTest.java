package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.format.parser.ModelParser;
import com.elfmcys.ysm.format.parser.RawCompileResult;
import com.elfmcys.ysm.format.vfs.Directory;
import com.elfmcys.ysm.model.catalog.CatalogModelLocation;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.domain.ModelPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvertedObjectStoreTest {
    @TempDir
    static Path fixtureTemp;

    @TempDir
    Path temp;

    private static Path fixture;
    private static ModelHash fixtureHash;

    @BeforeAll
    static void createFixture() throws Exception {
        var manifest = ConvertedObjectStoreTest.class.getResource(
                "/assets/ysm/builtin/default/ysm.json");
        var source = Path.of(java.util.Objects.requireNonNull(manifest).toURI()).getParent();
        try (var vfs = new Directory(source)) {
            fixture = ModelParser.parse(vfs, Files.createDirectories(fixtureTemp.resolve("model")),
                    com.elfmcys.ysm.format.parser.DefaultAnimationFilter.keepAll());
        }
        fixtureHash = ModelFileHandle.openDirect(fixture, location("fixture"))
                .descriptor().modelHash();
    }

    @Test
    void knownHashHitSkipsCompiler() throws Exception {
        var store = store(temp.resolve("hit"));
        store.commit(new RawCompileResult(fixtureHash, fixture), location("default"));
        var conversions = new AtomicInteger();

        var object = store.resolveKnownHash(temp.resolve("unused"), location("default"),
                fixtureHash, (source, output) -> {
                    conversions.incrementAndGet();
                    throw new AssertionError("compiler must not run");
                });

        assertEquals(fixtureHash, object.key().modelHash());
        assertEquals(0, conversions.get());
    }

    @Test
    void knownHashMissCompilesOnceAcrossConcurrentCallers() throws Exception {
        var store = store(temp.resolve("miss"));
        var conversions = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(4);
        try {
            var tasks = new ArrayList<java.util.concurrent.Callable<VerifiedConvertedObject>>();
            for (var index = 0; index < 4; index++) {
                tasks.add(() -> store.resolveKnownHash(temp.resolve("raw"), location("default"),
                        fixtureHash, copyingCompiler(conversions)));
            }
            for (var result : executor.invokeAll(tasks)) {
                assertEquals(fixtureHash, result.get().key().modelHash());
            }
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, conversions.get());
    }

    @Test
    void corruptObjectIsQuarantinedAndRebuilt() throws Exception {
        var paths = new SharedCachePaths(temp.resolve("corrupt"));
        var profile = testProfile();
        Files.createDirectories(paths.convertedObjects(profile));
        Files.writeString(paths.convertedObjects(profile).resolve(fixtureHash + ".mxc"), "corrupt");
        var conversions = new AtomicInteger();
        var store = new ConvertedObjectStore(paths, new AtomicSharedCache(paths), profile);

        var object = store.resolveKnownHash(temp.resolve("raw"), location("default"),
                fixtureHash, copyingCompiler(conversions));

        assertEquals(fixtureHash, object.key().modelHash());
        assertEquals(1, conversions.get());
        try (var files = Files.list(paths.convertedObjects(profile))) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString()
                    .startsWith(fixtureHash + ".mxc.corrupt-")));
        }
    }

    @Test
    void hashMismatchNeverCommitsExpectedObject() throws Exception {
        var paths = new SharedCachePaths(temp.resolve("mismatch"));
        var store = new ConvertedObjectStore(paths, new AtomicSharedCache(paths),
                testProfile());
        var bytes = fixtureHash.bytes();
        bytes[0] ^= 1;
        var expected = new ModelHash(bytes);

        assertThrows(ModelHashMismatchException.class, () -> store.resolveKnownHash(
                temp.resolve("raw"), location("default"), expected,
                copyingCompiler(new AtomicInteger())));
        assertFalse(Files.exists(paths.convertedObjects(testProfile())
                .resolve(expected + ".mxc")));
    }

    @Test
    void backingFailureInvalidationDefeatsStableStampValidationMemo() throws Exception {
        var paths = new SharedCachePaths(temp.resolve("memo"));
        var profile = testProfile();
        var store = new ConvertedObjectStore(paths, new AtomicSharedCache(paths), profile);
        store.commit(new RawCompileResult(fixtureHash, fixture), location("default"));
        var object = paths.convertedObjects(profile).resolve(fixtureHash + ".mxc");
        var modified = Files.getLastModifiedTime(object);
        var bytes = Files.readAllBytes(object);
        bytes[0] ^= 1;
        Files.write(object, bytes);
        Files.setLastModifiedTime(object, FileTime.fromMillis(modified.toMillis()));

        assertTrue(store.openVerified(fixtureHash, location("default")).isPresent());
        store.invalidate(fixtureHash);
        assertTrue(store.openVerified(fixtureHash, location("default")).isEmpty());
    }

    private ConvertedObjectStore store(Path root) {
        var paths = new SharedCachePaths(root);
        return new ConvertedObjectStore(paths, new AtomicSharedCache(paths),
                testProfile());
    }

    private static ConvertedObjectStore.RawCompiler copyingCompiler(AtomicInteger conversions) {
        return (source, output) -> {
            conversions.incrementAndGet();
            var converted = output.resolve(fixture.getFileName());
            Files.copy(fixture, converted);
            return new RawCompileResult(fixtureHash, converted);
        };
    }

    private static CatalogModelLocation location(String path) {
        return new CatalogModelLocation(CatalogRootKind.BUILTIN, new ModelPath(path));
    }

    private static ConversionProfileId testProfile() {
        return ConversionProfileId.from(ConversionProfileInputs.production(
                new com.elfmcys.ysm.model.domain.ModelHash(new byte[32])));
    }
}
