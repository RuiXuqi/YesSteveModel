package com.elfmcys.ysm.task;

import com.elfmcys.ysm.util.Closeable;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executor;

final class TaskNopContext implements TaskScope {
    TaskNopContext() {}

    @Override
    public boolean cancelled() {
        return true;
    }

    @Override
    public Executor executor() {
        return MoreExecutors.directExecutor();
    }

    @Override
    public boolean immediate() {
        return true;
    }

    @Override
    public TaskScope derive(boolean immediately) {
        return this;
    }

    @Override
    public Closeable guard(AutoCloseable action) {
        try {
            action.close();
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public void close() {}
}
