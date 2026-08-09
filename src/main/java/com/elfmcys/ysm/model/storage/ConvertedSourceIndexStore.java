package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.model.catalog.CatalogRootIdentity;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.catalog.ModelSourceKey;
import com.elfmcys.ysm.model.catalog.ModelSourceKind;
import com.elfmcys.ysm.model.catalog.SourceStamp;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.ModelPath;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/** Profile-scoped persistent source-to-object hints. */
public final class ConvertedSourceIndexStore {
    private static final int MAGIC = 0x59534D49;
    private static final int VERSION = 1;

    private final SharedCachePaths paths;
    private final AtomicSharedCache cache;
    private final ConversionProfileId profile;
    private final ConvertedCacheLifecycle lifecycle;

    public ConvertedSourceIndexStore(SharedCachePaths paths, AtomicSharedCache cache,
                                     ConversionProfileId profile) {
        this(paths, cache, profile, null);
    }

    public ConvertedSourceIndexStore(SharedCachePaths paths, AtomicSharedCache cache,
                                     ConversionProfileId profile,
                                     ConvertedCacheLifecycle lifecycle) {
        this.paths = Objects.requireNonNull(paths, "paths");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.lifecycle = lifecycle;
    }

    public Optional<ConvertedSourceIndex> read(ModelSourceKey key) throws IOException {
        requireShared();
        var path = path(key);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            var value = readFile(path);
            return value.profile().equals(profile) && value.sourceKey().equals(key)
                    ? Optional.of(value) : Optional.empty();
        } catch (MalformedIndexException | IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    public void write(ConvertedSourceIndex value) throws IOException {
        requireShared();
        if (!value.profile().equals(profile)) {
            throw new IllegalArgumentException("Converted source index profile mismatch");
        }
        var path = path(value.sourceKey());
        var key = profile + "/" + keyDigest(value.sourceKey());
        cache.materialize("converted-index", key, path,
                candidate -> {
                    try {
                        return value.equals(readFile(candidate));
                    } catch (MalformedIndexException invalid) {
                        return false;
                    }
                },
                candidate -> writeFile(candidate, value));
    }

    public void delete(ModelSourceKey key) throws IOException {
        requireShared();
        var path = cache.checkedTarget(path(key));
        cache.withKeyLock("converted-index", profile + "/" + keyDigest(key), () -> {
            Files.deleteIfExists(path);
            return null;
        });
    }

    public Path path(ModelSourceKey key) {
        return paths.convertedIndex(profile).resolve(key.root().rootKind().namespace())
                .resolve(keyDigest(key) + ".idx");
    }

    Optional<ConvertedSourceIndex> readPath(Path path) throws IOException {
        try {
            var value = readFile(path);
            return value.profile().equals(profile) ? Optional.of(value) : Optional.empty();
        } catch (MalformedIndexException | IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    private ConvertedSourceIndex readFile(Path path) throws IOException {
        var bytes = Files.readAllBytes(path);
        try (var input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("Unsupported converted source index");
            }
            var storedProfile = new byte[Hash256.SIZE];
            input.readFully(storedProfile);
            var rootKind = CatalogRootKind.values()[input.readUnsignedByte()];
            var root = new CatalogRootIdentity(rootKind, Path.of(readString(input)),
                    readString(input));
            var sourceKind = ModelSourceKind.values()[input.readUnsignedByte()];
            var sourceKey = new ModelSourceKey(root, new ModelPath(readString(input)), sourceKind);
            var stamp = readStamp(input);
            var modelHash = new byte[Hash256.SIZE];
            var descriptorHash = new byte[Hash256.SIZE];
            input.readFully(modelHash);
            input.readFully(descriptorHash);
            if (input.read() != -1) {
                throw new IOException("Trailing converted source index data");
            }
            return new ConvertedSourceIndex(sourceKey, stamp,
                    new ConversionProfileId(new Hash256(storedProfile)),
                    new Hash256(modelHash), new Hash256(descriptorHash));
        } catch (IOException | IllegalArgumentException | ArrayIndexOutOfBoundsException error) {
            throw new MalformedIndexException(error);
        }
    }

    private static void writeFile(Path path, ConvertedSourceIndex value) throws IOException {
        try (var output = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(path)))) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.write(value.profile().hash().bytes());
            output.writeByte(value.sourceKey().root().rootKind().ordinal());
            writeString(output, value.sourceKey().root().canonicalAbsoluteRoot().toString());
            writeString(output, value.sourceKey().root().fileKey());
            output.writeByte(value.sourceKey().sourceKind().ordinal());
            writeString(output, value.sourceKey().relativePath().value());
            writeStamp(output, value.lastObservedStamp());
            output.write(value.modelHash().bytes());
            output.write(value.descriptorHash().bytes());
        }
    }

    private static void writeStamp(DataOutputStream output, SourceStamp stamp) throws IOException {
        if (stamp instanceof SourceStamp.File file) {
            output.writeByte(1);
            output.writeLong(file.size());
            output.writeLong(file.lastModifiedMillis());
            writeString(output, file.fileKey());
        } else if (stamp instanceof SourceStamp.RawDirectory directory) {
            output.writeByte(2);
            output.write(directory.metadataDigest().bytes());
            output.writeInt(directory.fileCount());
            output.writeLong(directory.totalBytes());
        } else {
            throw new IOException("Unsupported source stamp type");
        }
    }

    private static SourceStamp readStamp(DataInputStream input) throws IOException {
        return switch (input.readUnsignedByte()) {
            case 1 -> new SourceStamp.File(input.readLong(), input.readLong(), readString(input));
            case 2 -> {
                var digest = new byte[Hash256.SIZE];
                input.readFully(digest);
                yield new SourceStamp.RawDirectory(new Hash256(digest), input.readInt(),
                        input.readLong());
            }
            default -> throw new IOException("Unsupported converted source stamp");
        };
    }

    private static String keyDigest(ModelSourceKey key) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var output = new DataOutputStream(bytes)) {
                output.writeByte(key.root().rootKind().ordinal());
                writeString(output, key.root().canonicalAbsoluteRoot().toString());
                writeString(output, key.root().fileKey());
                output.writeByte(key.sourceKind().ordinal());
                writeString(output, key.relativePath().value());
            }
            return HexFormat.of().formatHex(sha256(bytes.toByteArray()));
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input) throws IOException {
        var length = input.readInt();
        if (length < 0 || length > 1024 * 1024) {
            throw new IOException("Invalid converted source index string length");
        }
        return new String(input.readNBytes(length), StandardCharsets.UTF_8);
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }

    private void requireShared() {
        if (lifecycle != null) {
            lifecycle.requireShared();
        }
    }

    private static final class MalformedIndexException extends IOException {
        private MalformedIndexException(Throwable cause) {
            super("Malformed converted source index", cause);
        }
    }
}
