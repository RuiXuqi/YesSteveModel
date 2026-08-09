package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.format.parser.RawCompileResult;
import com.elfmcys.ysm.model.catalog.CatalogModelLocation;
import com.elfmcys.ysm.model.domain.Hash256;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Profile-scoped immutable converted objects with metadata-last visibility. */
public final class ConvertedObjectStore {
    private static final String LOCK_NAMESPACE = "converted-object";

    private final SharedCachePaths paths;
    private final AtomicSharedCache cache;
    private final ConversionProfileId profile;
    private final ConvertedCacheLifecycle lifecycle;
    private final ConcurrentHashMap<Hash256, ValidationMemo> validationMemos =
            new ConcurrentHashMap<>();

    public ConvertedObjectStore(SharedCachePaths paths, AtomicSharedCache cache,
                                ConversionProfileId profile) {
        this(paths, cache, profile, null);
    }

    public ConvertedObjectStore(SharedCachePaths paths, AtomicSharedCache cache,
                                ConversionProfileId profile,
                                ConvertedCacheLifecycle lifecycle) {
        this.paths = Objects.requireNonNull(paths, "paths");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.lifecycle = lifecycle;
    }

    public ConversionProfileId profile() {
        return profile;
    }

    public Path temporaryRoot() {
        return paths.convertedTemporary();
    }

    public Optional<VerifiedConvertedObject> openVerified(Hash256 modelHash,
                                                          CatalogModelLocation location)
            throws IOException {
        requireShared();
        return Optional.ofNullable(openIfValid(new ConvertedObjectKey(profile, modelHash), location));
    }

    public boolean isStructurallyVisible(Hash256 modelHash) throws IOException {
        requireShared();
        var key = new ConvertedObjectKey(profile, modelHash);
        var object = cache.checkedTarget(objectPath(key));
        var metadataFile = cache.checkedTarget(metadataPath(key));
        if (!Files.isRegularFile(object) || !Files.isRegularFile(metadataFile)) {
            return false;
        }
        try {
            var metadata = ConvertedObjectMetadata.read(metadataFile);
            return metadata.key().equals(key) && metadata.containerSize() == Files.size(object);
        } catch (IOException | RuntimeException invalid) {
            return false;
        }
    }

    public VerifiedConvertedObject commit(RawCompileResult compiled,
                                           CatalogModelLocation location) throws IOException {
        requireShared();
        var key = new ConvertedObjectKey(profile, compiled.modelHash());
        return cache.withKeyLock(LOCK_NAMESPACE, key.toString(),
                () -> commitLocked(key, compiled, location));
    }

    public VerifiedConvertedObject resolveKnownHash(Path source, CatalogModelLocation location,
                                                    Hash256 expectedHash, RawCompiler compiler)
            throws IOException {
        requireShared();
        var key = new ConvertedObjectKey(profile, expectedHash);
        var existing = openIfValid(key, location);
        if (existing != null) {
            return existing;
        }
        return cache.withKeyLock(LOCK_NAMESPACE, key.toString(), () -> {
            var afterLock = openIfValid(key, location);
            if (afterLock != null) {
                return afterLock;
            }
            Files.createDirectories(paths.convertedTemporary());
            var temporaryDirectory = Files.createTempDirectory(
                    paths.convertedTemporary(), "known-");
            RawCompileResult compiled = null;
            try {
                compiled = compiler.compile(source, temporaryDirectory);
                if (!compiled.modelHash().equals(expectedHash)) {
                    throw new ModelHashMismatchException(location, expectedHash,
                            compiled.modelHash());
                }
                return commitLocked(key, compiled, location);
            } finally {
                if (compiled != null && compiled.stagedContainer().startsWith(temporaryDirectory)) {
                    Files.deleteIfExists(compiled.stagedContainer());
                }
                deleteEmptyDirectory(temporaryDirectory);
            }
        });
    }

    public void invalidate(Hash256 modelHash) {
        requireShared();
        validationMemos.remove(modelHash);
    }

    Path objectPath(ConvertedObjectKey key) {
        return paths.convertedObjects(key.profile()).resolve(key.modelHash() + ".mxc");
    }

    Path metadataPath(ConvertedObjectKey key) {
        return paths.convertedObjects(key.profile()).resolve(key.modelHash() + ".meta");
    }

    private VerifiedConvertedObject commitLocked(ConvertedObjectKey key,
                                                  RawCompileResult compiled,
                                                  CatalogModelLocation location) throws IOException {
        var existing = openIfValid(key, location);
        if (existing != null) {
            return existing;
        }
        if (!key.modelHash().equals(compiled.modelHash())) {
            throw new IOException("Compiled model hash does not match converted object key");
        }

        var object = cache.checkedTarget(objectPath(key));
        var metadata = cache.checkedTarget(metadataPath(key));
        validationMemos.remove(key.modelHash());
        quarantinePair(object, metadata);
        Files.createDirectories(object.getParent());

        var suffix = ".tmp-" + ProcessHandle.current().pid() + "-" + UUID.randomUUID();
        var objectTemporary = object.resolveSibling(object.getFileName() + suffix);
        var metadataTemporary = metadata.resolveSibling(metadata.getFileName() + suffix);
        var startedAt = System.nanoTime();
        try {
            Files.copy(compiled.stagedContainer(), objectTemporary,
                    StandardCopyOption.REPLACE_EXISTING);
            var validated = ModelFileHandle.openDirect(objectTemporary, location);
            if (!validated.descriptor().modelHash().equals(key.modelHash())) {
                throw new ModelHashMismatchException(location, key.modelHash(),
                        validated.descriptor().modelHash());
            }
            var gate = new ConvertedObjectMetadata(key,
                    validated.descriptor().descriptorHash(), Files.size(objectTemporary));
            ConvertedObjectMetadata.write(metadataTemporary, gate);
            if (!gate.equals(ConvertedObjectMetadata.read(metadataTemporary))) {
                throw new IOException("Converted object metadata failed round-trip validation");
            }

            AtomicSharedCache.moveCommitted(objectTemporary, object);
            AtomicSharedCache.moveCommitted(metadataTemporary, metadata);
            var committed = requireValid(key, location);
            YesSteveModel.LOGGER.debug(
                    "Committed converted object key={} elapsedMs={}", key,
                    Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
            return committed;
        } finally {
            Files.deleteIfExists(objectTemporary);
            Files.deleteIfExists(metadataTemporary);
        }
    }

    private VerifiedConvertedObject requireValid(ConvertedObjectKey key,
                                                 CatalogModelLocation location) throws IOException {
        var value = openIfValid(key, location);
        if (value == null) {
            throw new IOException("Committed converted object is not visible: " + key);
        }
        return value;
    }

    private VerifiedConvertedObject openIfValid(ConvertedObjectKey key,
                                                CatalogModelLocation location) throws IOException {
        var object = cache.checkedTarget(objectPath(key));
        var metadataFile = cache.checkedTarget(metadataPath(key));
        if (!Files.isRegularFile(object) || !Files.isRegularFile(metadataFile)) {
            return null;
        }
        try {
            var metadata = ConvertedObjectMetadata.read(metadataFile);
            if (!metadata.key().equals(key) || metadata.containerSize() != Files.size(object)) {
                validationMemos.remove(key.modelHash());
                return null;
            }
            var objectStamp = FileStamp.capture(object);
            var metadataStamp = FileStamp.capture(metadataFile);
            var memo = validationMemos.get(key.modelHash());
            if (memo != null && memo.key().equals(key)
                    && memo.objectStamp().equals(objectStamp)
                    && memo.metadataStamp().equals(metadataStamp)
                    && memo.descriptor().descriptorHash().equals(metadata.descriptorHash())) {
                return new VerifiedConvertedObject(key, object, memo.descriptor());
            }
            var validated = ModelFileHandle.openDirect(object, location);
            if (!validated.descriptor().modelHash().equals(key.modelHash())
                    || !validated.descriptor().descriptorHash().equals(metadata.descriptorHash())) {
                validationMemos.remove(key.modelHash());
                return null;
            }
            validationMemos.put(key.modelHash(), new ValidationMemo(key, objectStamp,
                    metadataStamp, validated.descriptor()));
            var handle = ModelFileHandle.openConverted(object, location);
            return new VerifiedConvertedObject(key, object, handle.descriptor());
        } catch (IOException | RuntimeException invalid) {
            validationMemos.remove(key.modelHash());
            YesSteveModel.LOGGER.debug("Converted object validation failed key={}", key, invalid);
            return null;
        }
    }

    private static void quarantinePair(Path object, Path metadata) throws IOException {
        var suffix = ".corrupt-" + Instant.now().toEpochMilli();
        quarantine(object, suffix);
        quarantine(metadata, suffix);
    }

    private static void quarantine(Path file, String suffix) throws IOException {
        if (Files.exists(file)) {
            Files.move(file, file.resolveSibling(file.getFileName() + suffix),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteEmptyDirectory(Path directory) throws IOException {
        if (Files.isDirectory(directory)) {
            try (var entries = Files.list(directory)) {
                if (entries.findAny().isEmpty()) {
                    Files.deleteIfExists(directory);
                }
            }
        }
    }

    private void requireShared() {
        if (lifecycle != null) {
            lifecycle.requireShared();
        }
    }

    @FunctionalInterface
    public interface RawCompiler {
        RawCompileResult compile(Path source, Path outputDirectory) throws IOException;
    }

    private record ValidationMemo(ConvertedObjectKey key, FileStamp objectStamp,
                                  FileStamp metadataStamp,
                                  com.elfmcys.ysm.model.domain.ModelDescriptor descriptor) {
    }

    private record FileStamp(long size, long modifiedMillis, String fileKey) {
        private static FileStamp capture(Path path) throws IOException {
            var attributes = Files.readAttributes(path, BasicFileAttributes.class);
            return new FileStamp(attributes.size(), attributes.lastModifiedTime().toMillis(),
                    Objects.toString(attributes.fileKey(), ""));
        }
    }
}
