package com.elfmcys.ysm.buffer;

import com.elfmcys.ysm.util.UnsafeUtil;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;

// 只读
class MappedFileBuffer implements NativeBuffer {
    private final MappedByteBuffer mappedRegion;
    private final FileChannel file;
    private final ByteBuffer data;
    private final long headPtr;

    private final AtomicInteger refCounter;

    MappedFileBuffer(FileChannel file, long offset, long size) throws IOException {
        this.mappedRegion = file.map(FileChannel.MapMode.READ_ONLY, offset, size);
        this.file = file;
        this.data = mappedRegion.duplicate();
        this.headPtr = MemoryUtil.memAddress(mappedRegion);

        this.refCounter = new AtomicInteger(1);
    }

    private MappedFileBuffer(MappedByteBuffer mappedRegion, FileChannel file, ByteBuffer data, long headPtr, AtomicInteger refCounter) {
        this.mappedRegion = mappedRegion;
        this.file = file;
        this.data = data;
        this.headPtr = headPtr;
        this.refCounter = refCounter;
    }

    private void checkClosed() {
        if (refCounter.getAcquire() < 1) {
            throw new IllegalStateException("Buffer has been closed");
        }
    }

    @Override
    public NativeBuffer acquire() {
        checkClosed();
        refCounter.incrementAndGet();
        return this;
    }

    @Override
    public MappedFileBuffer slice(int offset, int size) {
        checkClosed();

        int currentSize = data.remaining();
        if (offset < 0 || size < 0 || offset > currentSize - size) {
            throw new IndexOutOfBoundsException();
        }

        var headPtr = this.headPtr + offset;
        var data = MemoryUtil.memByteBuffer(headPtr, size);
        return new MappedFileBuffer(mappedRegion, file, data, headPtr, refCounter);
    }

    @Override
    public ByteBuffer nio() {
        checkClosed();
        return data.asReadOnlyBuffer();
    }

    @Override
    public long ptr() {
        checkClosed();
        return headPtr;
    }

    @Override
    public int size() {
        return mappedRegion.remaining();
    }

    @Override
    public void close() {
        if (refCounter.decrementAndGet() == 0) {
            UnsafeUtil.getUnsafe().invokeCleaner(mappedRegion);
        }
    }
}
