package com.elfmcys.yesstevemodel.molang.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Array extends ScopedIterable {
    @Nullable Object getElement(final @NotNull ExecutionContext<?> context, int index);
}
