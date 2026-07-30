package com.elfmcys.ysm.task;

import com.elfmcys.ysm.util.Closeable;

import java.util.concurrent.Executor;

public interface TaskScope extends Closeable, TaskContext {
    static TaskScope create(Executor executor, boolean immediate) {
        return new TaskScopeImpl(immediate, executor);
    }

    static TaskScope create(Executor executor) {
        return new TaskScopeImpl(false, executor);
    }
}
