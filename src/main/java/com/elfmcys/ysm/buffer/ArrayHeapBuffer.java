package com.elfmcys.ysm.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;

class ArrayHeapBuffer implements ArrayBuffer {
    private final ByteBuf data;
    private final boolean owning;

    ArrayHeapBuffer(int size, boolean pooled) {
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("Size too large");
        }
        data = pooled ? PooledByteBufAllocator.DEFAULT.heapBuffer(size, size).writerIndex(size) :
                Unpooled.wrappedBuffer(new byte[size], 0, size).writerIndex(size);
        owning = true;
    }

    ArrayHeapBuffer(byte[] array, boolean owning) {
        data = Unpooled.wrappedBuffer(array, 0, array.length)
                .writerIndex(array.length);
        this.owning = owning;
    }

    ArrayHeapBuffer(ByteBuffer buffer, boolean owning) {
        if (!buffer.hasArray()) {
            throw new IllegalArgumentException("Buffer has no backing array");
        }
        data = Unpooled.wrappedBuffer(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining())
                .writerIndex(buffer.remaining());
        this.owning = owning;
    }

    ArrayHeapBuffer(byte[] array, int offset, int size, boolean owning) {
        if (array == null) {
            throw new NullPointerException("array");
        }
        if (offset < 0 || size < 0 || size > array.length - offset) {
            throw new IndexOutOfBoundsException();
        }
        data = Unpooled.wrappedBuffer(array, offset, size).writerIndex(size);
        this.owning = owning;
    }

    private ArrayHeapBuffer(ByteBuf data, boolean owning) {
        this.data = data;
        this.owning = owning;
    }

    private void checkClosed() {
        if (owning && data.refCnt() <= 0) {
            throw new IllegalStateException("Buffer is closed");
        }
    }

    @Override
    public ArrayBuffer acquire() {
        if (owning) {
            return new ArrayHeapBuffer(data.retainedSlice(), true);
        } else {
            return copy();
        }
    }

    @Override
    public ArrayBuffer slice(int offset, int size) {
        checkClosed();

        int currentSize = data.readableBytes();
        if (offset < 0 || size < 0 || offset > currentSize - size) {
            throw new IndexOutOfBoundsException();
        }

        return new ArrayHeapBuffer(this.data.slice(offset, size).writerIndex(size), owning);
    }

    @Override
    public ByteBuffer nio() {
        checkClosed();
        return data.nioBuffer(data.readerIndex(), data.readableBytes());
    }

    public byte[] array() {
        checkClosed();
        return data.array();
    }

    public int arrayOffset() {
        return data.arrayOffset() + data.readerIndex();
    }

    @Override
    public int size() {
        return data.readableBytes();
    }

    @Override
    public void close() {
        if (owning) {
            data.release();
        }
    }
}
