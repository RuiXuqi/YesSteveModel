package com.elfmcys.yesstevemodel.client.compat.bettercombat;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.util.PersonView;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import net.minecraftforge.fml.loading.LoadingModList;

public class BetterCombatCompat {
    private static final String MOD_ID = "bettercombat";
    private static boolean INSTALLED;

    public static void init() {
        INSTALLED = LoadingModList.get().getModFileById(MOD_ID) != null;
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static boolean shouldHideHead(CustomPlayerEntity entity) {
        return INSTALLED && PersonView.isFirstPersonView(entity) && !RenderUtil.isRenderingEntitiesInPaperDoll();
    }
}
