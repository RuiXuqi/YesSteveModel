package com.elfmcys.ysm.util;

public interface Closeable extends AutoCloseable {
    @Override
    void close();
}
