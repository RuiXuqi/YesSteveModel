package com.elfmcys.ysm.natives;

import java.io.Closeable;

public final class NativeProfiler {
    private static final int ZONE_ANIMATABLE_ENTITY_UPDATE = 0;
    private static final int ZONE_FALLBACK_VERTEX_WRITER_WRITE = 1;

    private NativeProfiler() {
    }

    public static Scope beginAnimatableUpdate() {
        return beginZone(ZONE_ANIMATABLE_ENTITY_UPDATE);
    }

    public static Scope beginFallbackVertexWrite() {
        return beginZone(ZONE_FALLBACK_VERTEX_WRITER_WRITE);
    }

    public static boolean beginFrame() {
        return NativeRuntime.isTracyEnabled() && nBeginFrame() != 0;
    }

    public static void endFrame(boolean active) {
        if (active) {
            nEndFrame();
        }
    }

    private static Scope beginZone(int zoneId) {
        if (!NativeRuntime.isTracyEnabled()) {
            return Scope.NOOP;
        }
        var token = nBeginZone(zoneId);
        return token == 0 ? Scope.NOOP : new Scope(token);
    }

    public static final class Scope implements Closeable {
        private static final Scope NOOP = new Scope(0);

        private final long token;

        private Scope(long token) {
            this.token = token;
        }

        @Override
        public void close() {
            if (token != 0) {
                nEndZone(token);
            }
        }
    }

    private static native long nBeginZone(int zoneId);

    private static native void nEndZone(long token);

    private static native long nBeginFrame();

    private static native void nEndFrame();
}
