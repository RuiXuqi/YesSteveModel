package com.elfmcys.ysm.client.model;

import java.lang.ref.Reference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

public final class ModelRenderTargetLease implements AutoCloseable {
    private final ModelRenderTarget renderTarget;
    private final Runnable release;
    private final BooleanSupplier current;
    private final AtomicBoolean closed = new AtomicBoolean();

    public ModelRenderTargetLease(ModelRenderTarget renderTarget, Runnable release,
                                  BooleanSupplier current) {
        this.renderTarget = Objects.requireNonNull(renderTarget, "renderTarget");
        this.release = Objects.requireNonNull(release, "release");
        this.current = Objects.requireNonNull(current, "current");
    }

    public ModelRenderTarget renderTarget() {
        return renderTarget;
    }

    public boolean isCurrent() {
        return !closed.get() && current.getAsBoolean();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            release.run();
        }
    }
}
