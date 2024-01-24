package com.elfmcys.yesstevemodel.util;

import io.netty.util.internal.ObjectCleaner;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

// Native Access
public class CleanerUtil {
    public static <T> void ref(T obj, Consumer<T> cleanAction) {
        final WeakReference<T> weakRef = new WeakReference<>(obj);
        ObjectCleaner.register(obj, () -> cleanAction.accept(weakRef.get()));
    }
}