package com.elfmcys.ysm.model;

/** Lifecycle bridge between Forge common setup and the process model-system owner. */
public final class ModelRuntime {
    private static ModelSystem system;
    private static boolean shutdownHookRegistered;

    private ModelRuntime() {
    }

    public static synchronized void initialize() {
        if (system != null) {
            return;
        }
        var created = ModelSystem.openDefault();
        try {
            if (!shutdownHookRegistered) {
                Runtime.getRuntime().addShutdownHook(
                        new Thread(ModelRuntime::close, "YSM Model Runtime Shutdown"));
                shutdownHookRegistered = true;
            }
            system = created;
        } catch (RuntimeException | Error error) {
            try {
                created.close();
            } catch (RuntimeException closeError) {
                error.addSuppressed(closeError);
            }
            throw error;
        }
    }

    public static synchronized ModelSystem system() {
        if (system == null) {
            throw new IllegalStateException("Model runtime has not been initialized");
        }
        return system;
    }

    public static synchronized void close() {
        var current = system;
        system = null;
        if (current != null) {
            current.close();
        }
    }
}
