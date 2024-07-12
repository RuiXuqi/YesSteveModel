package com.elfmcys.yesstevemodel.client.compat.parcool;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.LoadingModList;
import org.jetbrains.annotations.Nullable;

public class ParCoolCompat {
    private static final String MOD_ID = "parcool";
    private static boolean INSTALLED;

    public static void init() {
        INSTALLED = LoadingModList.get().getModFileById(MOD_ID) != null;
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    @Nullable
    public static String getAnimation(Player player) {
        if (isInstalled()) {
            return ParCoolAnimationManger.getAnimation(player);
        }
        return null;
    }
}
