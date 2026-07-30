package com.elfmcys.ysm.buffer;

import com.elfmcys.ysm.util.ScopeGuard;

import java.nio.ByteBuffer;

public interface UniBuffer extends AutoCloseable {
    int MAX_SIZE = 256 * 1024 * 1024;

    ByteBuffer nio();

    BufferType type();

    int size();

    // 不应该暴露
    // boolean owning();

    // 注意不自增引用计数
    UniBuffer slice(int offset, int size);

    UniBuffer borrow();

    UniBuffer acquire();

    UniBuffer copy();

    NativeBuffer acquireNative();

    ArrayBuffer acquireArray();

    @Override
    void close();

    static UniBuffer move(ByteBuffer buffer) {
        if (buffer.isDirect()) {
            return NativeBuffer.move(buffer);
        } else if (buffer.hasArray()) {
            return ArrayBuffer.move(buffer);
        } else {
            throw new IllegalArgumentException();
        }
    }

    static UniBuffer borrow(ByteBuffer buffer) {
        if (buffer.isDirect()) {
            return NativeBuffer.borrow(buffer);
        } else if (buffer.hasArray()) {
            return ArrayBuffer.borrow(buffer);
        } else {
            throw new IllegalArgumentException();
        }
    }

    static UniBuffer allocate(int size, BufferType type) {
        if (type == BufferType.NATIVE) {
            return NativeBuffer.allocate(size);
        } else {
            return ArrayBuffer.allocate(size);
        }
    }

    static ScopeGuard<UniBuffer> allocateWithScope(int size, BufferType type) {
        return ScopeGuard.create(allocate(size, type));
    }
}
