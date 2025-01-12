package com.elfmcys.yesstevemodel.client.compat.parcool;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.LoadingModList;
import org.apache.commons.lang3.StringUtils;
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

    public static void addBinding(CtrlBinding binding) {
        if (isInstalled()) {
            ParCoolCtrlBinding.addInnerBinding(binding);
        } else {
            addEmptyBinding(binding);
        }
    }

    /**
     * 没有安装此模组时，这些 molang 应该存在，否则会报错
     */
    private static void addEmptyBinding(CtrlBinding binding) {
        binding.playerVar("parcool_state", ctx -> StringUtils.EMPTY);
    }
}
