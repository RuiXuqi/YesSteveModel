package com.elfmcys.yesstevemodel.client.compat.backpack.sophisticated;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.compat.swem.SwemCtrlBinding;
import com.elfmcys.yesstevemodel.client.event.RegisterEntityRenderersEvent;
import net.minecraftforge.fml.loading.LoadingModList;

public class SophisticatedCompat {
    private static final String MOD_ID = "sophisticatedbackpacks";
    private static boolean INSTALLED;

    /**
     * 需要在资源加载后初始化
     */
    public static void init() {
        INSTALLED = LoadingModList.get().getModFileById(MOD_ID) != null;
        if (isInstalled()) {
            RegisterEntityRenderersEvent.getPlayerRenderer().addLayer(new YsmBackpackLayerRenderer());
        }
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static void addBinding(CtrlBinding binding) {
        if (isInstalled()) {
            SophisticatedCtrlBinding.addInnerBinding(binding);
        } else {
            addEmptyBinding(binding);
        }
    }

    /**
     * 没有安装此模组时，这些 molang 应该存在，否则会报错
     */
    private static void addEmptyBinding(CtrlBinding binding) {
        binding.livingEntityVar("has_sophisticated_backpack", ctx -> false);
    }
}
