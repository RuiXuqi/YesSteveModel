package com.elfmcys.yesstevemodel.client.compat.backpack.sophisticated;

import com.elfmcys.yesstevemodel.client.event.RegisterEntityRenderersEvent;
import net.minecraftforge.fml.loading.LoadingModList;

public class SophisticatedCompat {
    private static final String MOD_ID = "sophisticatedbackpacks";
    private static boolean INSTALLED;

    public static void init() {
        INSTALLED = LoadingModList.get().getModFileById(MOD_ID) != null;
        if (isInstalled()) {
            RegisterEntityRenderersEvent.getPlayerRenderer().addLayer(new YsmBackpackLayerRenderer());
        }
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }
}
