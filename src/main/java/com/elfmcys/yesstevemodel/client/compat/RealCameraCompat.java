package com.elfmcys.yesstevemodel.client.compat;

import net.minecraftforge.fml.ModList;

public class RealCameraCompat {
    private static final String MOD_ID = "realcamera";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }
}
