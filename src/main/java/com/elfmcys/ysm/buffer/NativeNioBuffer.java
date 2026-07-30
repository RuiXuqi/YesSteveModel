package com.elfmcys.ysm.buffer;

import com.elfmcys.ysm.util.UnsafeUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

class NativeNioBuffer implements NativeBuffer {
    private final ByteBuffer owner;
    private final boolean owning;
    private final ByteBuffer data;
    private final long headPtr;

    private final AtomicInteger refCounter;

    public NativeNioBuffer(ByteBuffer data, boolean owning) {
        if (!data.isDirect()) {
            throw new IllegalArgumentException("Buffer is not direct");
        }
        this.owner = data.slice();
        this.owning = owning;
        this.data = owner;
        this.headPtr = MemoryUtil.memAddress(owner);
        this.refCounter = owning ? new AtomicInteger(1) : null;
    }

    public NativeNioBuffer(ByteBuffer owner, boolean owning, ByteBuffer data, long headPtr, AtomicInteger refCounter) {
        this.owner = owner;
        this.owning = owning;
        this.refCounter = refCounter;
        this.headPtr = headPtr;
        this.data = data;
    }

    private void checkClosed() {
        if (owning && refCounter.getAcquire() < 1) {
            throw new IllegalStateException("Buffer has been closed");
        }
    }

    @Override
    public NativeBuffer acquire() {
        checkClosed();
        if (owning) {
            refCounter.incrementAndGet();
            return this;
        } else {
            return copy();
        }
    }

    @Override
    public NativeBuffer slice(int offset, int size) {
        checkClosed();

        int currentSize = data.remaining();
        if (offset < 0 || size < 0 || offset > currentSize - size) {
            throw new IndexOutOfBoundsException();
        }

        var data = this.data.slice(offset, size);
        var headPtr = MemoryUtil.memAddress(data);

        return new NativeNioBuffer(owner, owning, data, headPtr, refCounter);
    }

    @Override
    public ByteBuffer nio() {
        checkClosed();
        return data.duplicate();
    }

    @Override
    public long ptr() {
        checkClosed();
        return headPtr;
    }

    @Override
    public int size() {
        return data.remaining();
    }

    @Override
    public void close() {
        if (owning && refCounter.decrementAndGet() == 0) {
            try {
                UnsafeUtil.getUnsafe().invokeCleaner(owner);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
