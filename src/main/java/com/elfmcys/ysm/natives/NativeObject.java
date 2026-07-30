package com.elfmcys.ysm.natives;

import com.elfmcys.ysm.util.Closeable;

import java.util.concurrent.atomic.AtomicBoolean;

public class NativeObject implements Closeable {
    private final long ptr;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public NativeObject(long ptr) {
        this.ptr = ptr;
    }

    public long get() {
        checkClosed();
        return ptr;
    }

    public NativeObject share() {
        checkClosed();
        var newPtr = nShare(ptr);
        if (newPtr == 0) {
            throw new RuntimeException("Failed to copy opaque ptr");
        }
        return new NativeObject(newPtr);
    }

    private void checkClosed() {
        if (closed.getAcquire()) {
            throw new IllegalStateException("Native object has been closed");
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            nDestroy(ptr);
        }
    }

    private static native long nShare(long ptr);
    private static native void nDestroy(long ptr);
}
