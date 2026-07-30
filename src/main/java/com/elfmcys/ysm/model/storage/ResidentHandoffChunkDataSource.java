package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.format.container.AssetContainerView;
import com.elfmcys.ysm.format.schema.file.ChunkDataSource;
import com.elfmcys.ysm.format.schema.file.ResidentChunkDataSource;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Stable chunk source for the builtin default model. Readers keep this object while its
 * backing is atomically promoted from the temporary container to resident chunk storage.
 */
final class ResidentHandoffChunkDataSource implements ChunkDataSource {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private ChunkDataSource delegate;

    ResidentHandoffChunkDataSource(ChunkDataSource delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    void promote(AssetContainerView view, Set<String> retainedTypes) throws IOException {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(retainedTypes, "retainedTypes");
        var writeLock = lock.writeLock();
        writeLock.lock();
        try {
            if (delegate instanceof ResidentChunkDataSource) {
                return;
            }
            var resident = ResidentChunkDataSource.copyOf(delegate, view, retainedTypes);
            delegate = resident;
        } finally {
            writeLock.unlock();
        }
    }

    boolean residentOnly() {
        var readLock = lock.readLock();
        readLock.lock();
        try {
            return delegate instanceof ResidentChunkDataSource;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public UniBuffer readPayload(AssetContainerView.ChunkInfo chunk,
                                 BufferType bufferType) throws IOException {
        var readLock = lock.readLock();
        readLock.lock();
        try {
            return delegate.readPayload(chunk, bufferType);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public UniBuffer readStoredVerified(AssetContainerView.ChunkInfo chunk,
                                        BufferType bufferType) throws IOException {
        var readLock = lock.readLock();
        readLock.lock();
        try {
            return delegate.readStoredVerified(chunk, bufferType);
        } finally {
            readLock.unlock();
        }
    }
}
