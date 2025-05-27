package com.elfmcys.yesstevemodel.molang.runtime;

import it.unimi.dsi.fastutil.objects.ObjectIterators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public interface Array extends ContextualIterable {
    Array EMPTY = new Array() {
        @Override
        public Iterator<?> iterator(@NotNull ExecutionContext<?> context) {
            return ObjectIterators.EMPTY_ITERATOR;
        }
        @Override
        public @Nullable Object getElement(@NotNull ExecutionContext<?> context, int index) {
            return null;
        }
    };

    @Nullable Object getElement(final @NotNull ExecutionContext<?> context, int index);
}
