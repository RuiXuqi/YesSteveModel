package com.elfmcys.yesstevemodel.client.compat.bettercombat;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.util.PersonView;
import net.minecraftforge.fml.loading.LoadingModList;
import org.apache.commons.lang3.StringUtils;

public class BetterCombatCompat {
    private static final String MOD_ID = "bettercombat";
    private static boolean INSTALLED;

    public static void init() {
        if (LoadingModList.get().getModFileById(MOD_ID) != null) {
            BetterCombatCompatInner.innerInit();
            INSTALLED = true;
        }
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static boolean shouldSkipRendering(CustomPlayerEntity entity) {
        return INSTALLED && PersonView.isFirstPersonView(entity);
    }

    public static void addBinding(CtrlBinding binding) {
        if (isInstalled()) {
            BetterCombatCompatInner.addInnerBinding(binding);
        } else {
            addEmptyBinding(binding);
        }
    }

    /**
     * 没有安装此模组时，这些 molang 应该存在，否则会报错
     */
    private static void addEmptyBinding(CtrlBinding binding) {
        binding.clientPlayerVar("bcombat_attack_animation", ctx -> StringUtils.EMPTY);
    }
}
