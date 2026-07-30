package com.elfmcys.ysm.natives.buffer;

import com.elfmcys.ysm.buffer.NativeBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;


public class NativeHeapBuffer implements NativeBuffer {
    private final long managedHeapPtr;
    private final long headPtr;
    private final ByteBuffer data;

    private final AtomicInteger refCounter;

    public NativeHeapBuffer(int size) {
        this(size, 0);
    }

    public NativeHeapBuffer(int size, int alignment) {
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("Size too large");
        }
        this.managedHeapPtr = nAlloc(size, alignment);
        if (managedHeapPtr == 0) {
            throw new OutOfMemoryError();
        }
        this.headPtr = managedHeapPtr;
        this.data = MemoryUtil.memByteBuffer(headPtr, size);
        this.refCounter = new AtomicInteger(1);
    }

    /**
     * mimalloc 无法检测任意指针指向其 heap region，需要自行确保
     */
    public NativeHeapBuffer(ByteBuffer ownedMem) {
        if (!ownedMem.isDirect()) {
            throw new IllegalArgumentException("Buffer is not direct");
        }
        this.managedHeapPtr = MemoryUtil.memAddressSafe(ownedMem) - ownedMem.position();
        this.headPtr = managedHeapPtr + ownedMem.position();
        this.data = MemoryUtil.memByteBuffer(headPtr, ownedMem.remaining());
        this.refCounter = new AtomicInteger(1);
    }

    private NativeHeapBuffer(long managedHeapPtr, long headPtr, ByteBuffer data, AtomicInteger refCounter) {
        this.managedHeapPtr = managedHeapPtr;
        this.headPtr = headPtr;
        this.data = data;
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

    public NativeHeapBuffer slice(int offset, int size) {
        checkClosed();

        if (offset < 0 || size < 0 || offset > data.remaining() - size) {
            throw new IndexOutOfBoundsException();
        }

        var headPtr = this.headPtr + offset;
        var data = MemoryUtil.memByteBuffer(headPtr, size);
        return new NativeHeapBuffer(managedHeapPtr, headPtr, data, refCounter);
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
        if (refCounter.decrementAndGet() == 0) {
            nFree(managedHeapPtr);
        }
    }

    private static native long nAlloc(int size, int alignment);

    private static native void nFree(long addr);
}
