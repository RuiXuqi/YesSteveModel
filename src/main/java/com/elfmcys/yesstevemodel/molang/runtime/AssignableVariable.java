package com.elfmcys.yesstevemodel.molang.runtime;

import javax.annotation.Nonnull;

public interface AssignableVariable extends Variable {
    void assign(final @Nonnull ExecutionContext<?> context, Object value);
}
