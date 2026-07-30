package com.elfmcys.ysm.buffer;

import com.elfmcys.ysm.util.ScopeGuard;
import us.hebi.quickbuf.RepeatedByte;

import java.nio.ByteBuffer;

public interface ArrayBuffer extends UniBuffer {
    byte[] array();

    int arrayOffset();

    @Override
    ArrayBuffer slice(int offset, int size);

    @Override
    default ArrayBuffer borrow() {
        return new ArrayHeapBuffer(array(), arrayOffset(), size(), false);
    }

    @Override
    ArrayBuffer acquire();

    @Override
    default ArrayBuffer copy() {
        var size = size();
        try (var bufScope = allocateWithScope(size)) {
            System.arraycopy(array(), arrayOffset(), bufScope.get().array(), bufScope.get().arrayOffset(), size);
            return bufScope.release();
        }
    }

    @Override
    default BufferType type() {
        return BufferType.ARRAY;
    }

    @Override
    default ArrayBuffer acquireArray() {
        return acquire();
    }

    @Override
    default NativeBuffer acquireNative() {
        var result = NativeBuffer.allocate(size());
        try {
            UniBufferIO.copy(this, 0, result, 0, size());
            return result;
        } catch (Throwable error) {
            result.close();
            throw error;
        }
    }

    static ArrayBuffer allocate(int size) {
        return new ArrayHeapBuffer(size, size > 1024);
    }

    static ScopeGuard<ArrayBuffer> allocateWithScope(int size) {
        return ScopeGuard.create(allocate(size));
    }

    static ArrayBuffer move(byte[] array) {
        return new ArrayHeapBuffer(array, true);
    }

    static ArrayBuffer move(ByteBuffer buffer) {
        return new ArrayHeapBuffer(buffer, true);
    }

    static ArrayBuffer move(RepeatedByte bytes) {
        return move(bytes.array(), 0, bytes.length());
    }

    static ArrayBuffer move(byte[] array, int offset, int size) {
        return new ArrayHeapBuffer(array, offset, size, true);
    }

    static ArrayBuffer borrow(byte[] array) {
        return new ArrayHeapBuffer(array, false);
    }

    static ArrayBuffer borrow(ByteBuffer buffer) {
        return new ArrayHeapBuffer(buffer, false);
    }

    static ArrayBuffer borrow(RepeatedByte bytes) {
        return borrow(bytes.array(), 0, bytes.length());
    }

    static ArrayBuffer borrow(byte[] array, int offset, int size) {
        return new ArrayHeapBuffer(array, offset, size, false);
    }

    static ArrayBuffer copyOf(ByteBuffer data) {
        var result = allocate(data.remaining());
        if (data.hasArray()) {
            System.arraycopy(data.array(), data.arrayOffset() + data.position(), result.array(), result.arrayOffset(), result.size());
        } else {
            result.nio().put(data.duplicate());
        }
        return result;
    }
}
