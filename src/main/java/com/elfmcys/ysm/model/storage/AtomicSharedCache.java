package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.YesSteveModel;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** Cross-process, immutable-object writer used by converted, remote and baked caches. */
public final class AtomicSharedCache {
    private static final ConcurrentHashMap<Path, JvmLock> JVM_LOCKS =
            new ConcurrentHashMap<>();

    private final SharedCachePaths paths;

    public AtomicSharedCache(SharedCachePaths paths) {
        this.paths = paths;
    }

    public void materialize(String namespace, String key, Path target,
                            CacheValidator validator, CacheWriter writer) throws IOException {
        target = checkedTarget(target);
        if (isValid(target, validator)) {
            YesSteveModel.LOGGER.debug("Shared cache hit namespace={} key={} target={}",
                    namespace, key, target);
            return;
        }

        var checked = target;
        withKeyLock(namespace, key, () -> {
                if (isValid(checked, validator)) {
                    YesSteveModel.LOGGER.debug(
                            "Shared cache hit after lock namespace={} key={} target={}",
                            namespace, key, checked);
                    return null;
                }
                if (quarantineInvalid(checked)) {
                    YesSteveModel.LOGGER.debug(
                            "Quarantined invalid shared cache object namespace={} key={} target={}",
                            namespace, key, checked);
                }
                Files.createDirectories(checked.getParent());
                Files.createDirectories(paths.temporary());

                var temporary = checked.resolveSibling(checked.getFileName() + ".tmp-"
                        + ProcessHandle.current().pid() + "-" + UUID.randomUUID());
                var startedAt = System.nanoTime();
                try {
                    writer.write(temporary);
                    if (!isValid(temporary, validator)) {
                        throw new IOException("Cache writer produced an invalid object: " + checked);
                    }
                    moveCommitted(temporary, checked);
                    YesSteveModel.LOGGER.debug(
                            "Materialized shared cache object namespace={} key={} target={} elapsedMs={}",
                            namespace, key, checked,
                            java.time.Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
                } finally {
                    Files.deleteIfExists(temporary);
                }
                return null;
        });
    }

    public <T> T withKeyLock(String namespace, String key, LockedOperation<T> operation)
            throws IOException {
        var lockFile = lockPath(namespace, key);
        Files.createDirectories(lockFile.getParent());
        var jvmLock = acquireJvmLock(lockFile);
        try {
            try (var lockChannel = FileChannel.open(lockFile,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 var ignored = lockChannel.lock()) {
                return operation.run();
            }
        } finally {
            releaseJvmLock(lockFile, jvmLock);
        }
    }

    Path checkedTarget(Path target) {
        var checked = target.toAbsolutePath().normalize();
        if (!checked.startsWith(paths.root())) {
            throw new IllegalArgumentException("Shared cache target escapes ~/.ysm/unstable: " + target);
        }
        var current = paths.root();
        if (Files.isSymbolicLink(current)) {
            throw new IllegalArgumentException("Shared cache root must not be a link: " + current);
        }
        for (var part : paths.root().relativize(checked)) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException(
                        "Shared cache target traverses a link: " + current);
            }
        }
        return checked;
    }

    private Path lockPath(String namespace, String key) {
        var digest = securityDigest((namespace + "\0" + key).getBytes(StandardCharsets.UTF_8));
        return paths.locks().resolve(sanitize(namespace)).resolve(HexFormat.of().formatHex(digest) + ".lck");
    }

    private static boolean isValid(Path path, CacheValidator validator) throws IOException {
        return Files.isRegularFile(path) && validator.validate(path);
    }

    static boolean quarantineInvalid(Path target) throws IOException {
        if (!Files.exists(target)) {
            return false;
        }
        var quarantine = target.resolveSibling(target.getFileName() + ".corrupt-" + Instant.now().toEpochMilli());
        Files.move(target, quarantine, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    static void moveCommitted(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    @FunctionalInterface
    public interface CacheValidator {
        boolean validate(Path path) throws IOException;
    }

    @FunctionalInterface
    public interface CacheWriter {
        void write(Path path) throws IOException;
    }

    @FunctionalInterface
    public interface LockedOperation<T> {
        T run() throws IOException;
    }

    private static byte[] securityDigest(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static JvmLock acquireJvmLock(Path path) {
        var value = JVM_LOCKS.compute(path, (ignored, current) -> {
            var result = current == null ? new JvmLock() : current;
            result.users++;
            return result;
        });
        value.lock.lock();
        return value;
    }

    private static void releaseJvmLock(Path path, JvmLock value) {
        value.lock.unlock();
        JVM_LOCKS.compute(path, (ignored, current) -> {
            if (current != value || value.users <= 0) {
                throw new IllegalStateException("Unbalanced shared-cache JVM lock");
            }
            value.users--;
            return value.users == 0 ? null : value;
        });
    }

    private static final class JvmLock {
        private final ReentrantLock lock = new ReentrantLock();
        private int users;
    }
}
