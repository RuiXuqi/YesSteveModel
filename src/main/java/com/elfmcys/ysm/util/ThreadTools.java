package com.elfmcys.ysm.util;

import java.util.concurrent.*;

public final class ThreadTools {
    private static final ThreadPoolExecutor THREAD_POOL = new ThreadPoolExecutor(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            30,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> {
                Thread thread = new Thread(runnable, "YSM Worker");
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.setDaemon(true);
                return thread;
            });

    public static Future<?> submit(Runnable runnable) {
        return THREAD_POOL.submit(runnable);
    }

    public static <T> Future<T> submit(Callable<T> runnable) {
        return THREAD_POOL.submit(runnable);
    }

    public static boolean safeSleep(int millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
