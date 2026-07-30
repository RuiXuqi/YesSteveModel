package com.elfmcys.ysm.task;

import com.elfmcys.ysm.util.Closeable;
import com.elfmcys.ysm.util.SpinLock;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

final class TaskScopeImpl implements TaskScope {
    @Nullable
    private final TaskScopeImpl parent;
    private final boolean immediate;
    private final Executor executor;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final SpinLock lock = new SpinLock();
    private final ReferenceOpenHashSet<AutoCloseable> children = new ReferenceOpenHashSet<>(4);

    TaskScopeImpl(boolean immediate, Executor executor) {
        this.parent = null;
        this.immediate = immediate;
        this.executor = executor;
    }

    private TaskScopeImpl(boolean immediate, Executor executor, @NotNull TaskScopeImpl parent) {
        this.parent = parent;
        this.immediate = immediate;
        this.executor = executor;
    }

    public boolean cancelled() {
        return cancelled.getAcquire() || (parent != null && parent.cancelled());
    }

    @Override
    public Executor executor() {
        return executor;
    }

    @Override
    public boolean immediate() {
        return immediate;
    }

    @Override
    public TaskScope derive(boolean immediate) {
        return lock.enter(() -> {
            if (!cancelled.getPlain()) {
                var child = new TaskScopeImpl(immediate, executor, this);
                children.add(child);
                return child;
            } else {
                return NOP;
            }
        });
    }

    public void cancel() {
        var copy = lock.enter(() -> {
            if (!cancelled.getPlain()) {
                cancelled.setRelease(true);
                var result = children.toArray(AutoCloseable[]::new);
                children.clear();
                return result;
            }
            return new AutoCloseable[0];
        });
        for (var child : copy) {
            try {
                child.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Nullable
    public Closeable guard(AutoCloseable action) {
        return lock.enter(() -> {
            if (!cancelled.getPlain()) {
                children.add(action);
                return () -> lock.enter(() -> children.remove(action));
            } else {
                try {
                    action.close();
                } catch (Exception ignored) {}
                return null;
            }
        });
    }

    @Override
    public void close() {
        cancel();
    }
}
