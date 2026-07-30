package com.elfmcys.ysm.natives;

import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.buffer.annotation.Borrowed;
import com.elfmcys.ysm.format.vfs.VirtualFileSystem;
import com.elfmcys.ysm.util.Closeable;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

// 目前支持 zip、7z、旧版 ysm
public class NativeArchive implements VirtualFileSystem, Closeable {
    private final long ptr;
    private boolean closed = false;

    public NativeArchive(String path) {
        ptr = nCreate(path);
        if (ptr == 0) {
            throw new IllegalArgumentException("Could not open archive: " + path);
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Already Closed");
        }
    }

    @Override
    public String[] listFiles(@Nullable String path) {
        checkClosed();
        var result = nList(ptr, path, 0);
        if (result == null) {
            throw new IllegalStateException("Failed to list files");
        }
        return result;
    }

    @Override
    public String[] listDirectories(@Nullable String path) {
        checkClosed();
        var result = nList(ptr, path, 1);
        if (result == null) {
            throw new IllegalStateException("Failed to list directories");
        }
        return result;
    }

    @Override
    public boolean hasFile(String fileName) {
        checkClosed();
        // 随便返回个东西
        return nGetFile(ptr, fileName, true) != null;
    }

    @Borrowed
    @Override
    public @Nullable NativeBuffer getFile(String fileName) {
        checkClosed();
        var buf = nGetFile(ptr, fileName, false);
        if (buf == null) {
            return null;
        }
        return NativeBuffer.borrow((ByteBuffer) buf);
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            nDestroy(ptr);
        }
    }

    private static native long nCreate(String path);
    private static native void nDestroy(long ptr);
    private static native String[] nList(long ptr, @Nullable String path, int type);
    private static native Object nGetFile(long ptr, String fileName, boolean dryRun);
}
