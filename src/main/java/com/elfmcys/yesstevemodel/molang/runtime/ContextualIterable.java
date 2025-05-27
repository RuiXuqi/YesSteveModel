package com.elfmcys.yesstevemodel.molang.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public interface ContextualIterable {
    Iterator<?> iterator(final @NotNull ExecutionContext<?> context);
}
