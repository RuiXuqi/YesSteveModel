package com.elfmcys.yesstevemodel.client.compat;

public class OptifineCompat {
    private static boolean INSTALLED;

    public static void init() {
        try {
            Class.forName("net.optifine.Config");
            INSTALLED = true;
        } catch (ClassNotFoundException ignore) {
            INSTALLED = false;
        }
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }
}
