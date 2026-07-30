package com.elfmcys.ysm.model.storage;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/** JVM owner of the process-shared converted-cache lifecycle lease. */
public final class ConvertedCacheLifecycle implements AutoCloseable {
    private final SharedCachePaths paths;
    private FileChannel channel;
    private FileLock lease;

    public ConvertedCacheLifecycle(SharedCachePaths paths) {
        this.paths = Objects.requireNonNull(paths, "paths");
    }

    public synchronized boolean tryAcquireStartupExclusive() throws IOException {
        requireUnleased();
        openChannel();
        try {
            lease = channel.tryLock(0, Long.MAX_VALUE, false);
        } catch (OverlappingFileLockException ignored) {
            lease = null;
        }
        if (lease == null) {
            closeChannel();
            return false;
        }
        return true;
    }

    public synchronized void releaseExclusive() throws IOException {
        if (lease == null || lease.isShared()) {
            throw new IllegalStateException("No converted-cache exclusive lease is held");
        }
        releaseLease();
        closeChannel();
    }

    public synchronized void acquireShared() throws IOException {
        requireUnleased();
        openChannel();
        try {
            lease = channel.lock(0, Long.MAX_VALUE, true);
        } catch (IOException | RuntimeException | Error error) {
            closeChannel();
            throw error;
        }
    }

    public synchronized boolean holdsShared() {
        return lease != null && lease.isValid() && lease.isShared();
    }

    public synchronized boolean holdsExclusive() {
        return lease != null && lease.isValid() && !lease.isShared();
    }

    public synchronized void requireShared() {
        if (!holdsShared()) {
            throw new IllegalStateException("Converted cache requires the JVM shared lifecycle lease");
        }
    }

    private void requireUnleased() {
        if (lease != null || channel != null) {
            throw new IllegalStateException("Converted-cache lifecycle lease is already open");
        }
    }

    private void openChannel() throws IOException {
        var lock = paths.convertedLifecycleLock();
        Files.createDirectories(lock.getParent());
        channel = FileChannel.open(lock, StandardOpenOption.CREATE, StandardOpenOption.READ,
                StandardOpenOption.WRITE);
    }

    private void releaseLease() throws IOException {
        if (lease != null) {
            lease.release();
            lease = null;
        }
    }

    private void closeChannel() throws IOException {
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }

    @Override
    public synchronized void close() throws IOException {
        IOException failure = null;
        try {
            releaseLease();
        } catch (IOException error) {
            failure = error;
        }
        try {
            closeChannel();
        } catch (IOException error) {
            if (failure == null) {
                failure = error;
            } else {
                failure.addSuppressed(error);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
