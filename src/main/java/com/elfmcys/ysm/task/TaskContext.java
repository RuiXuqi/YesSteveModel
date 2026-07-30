package com.elfmcys.ysm.task;

import com.elfmcys.ysm.util.Closeable;

import java.util.concurrent.Executor;

public interface TaskContext {
    TaskScope NOP = new TaskNopContext();

    boolean cancelled();

    Executor executor();

    boolean immediate();

    default TaskScope derive() {
        return derive(false);
    }

    TaskScope derive(boolean immediate);

    Closeable guard(AutoCloseable action);
}
