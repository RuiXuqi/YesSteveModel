package com.elfmcys.yesstevemodel.molang.runtime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Variable {
    @Nullable Object evaluate(final @Nonnull ExecutionContext<?> context);
}
