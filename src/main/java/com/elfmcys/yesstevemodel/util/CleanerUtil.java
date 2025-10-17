package com.elfmcys.yesstevemodel.util;

import io.netty.util.internal.ObjectCleaner;

import java.lang.ref.PhantomReference;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CleanerUtil {
    private final static WeakHashMap<Object, PhantomReference<Object>> REF_MAP = new WeakHashMap<>();

    public static <T> void ref(Object obj, T arg, Consumer<T> cleanAction) {
        ObjectCleaner.register(obj, () -> cleanAction.accept(arg));
    }

    public static <T0, T1> void ref(Object obj, T0 arg0, T1 arg1, BiConsumer<T0, T1> cleanAction) {
        ObjectCleaner.register(obj, () -> cleanAction.accept(arg0, arg1));
    }
}