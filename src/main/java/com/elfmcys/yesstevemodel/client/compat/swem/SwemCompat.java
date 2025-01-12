package com.elfmcys.yesstevemodel.client.compat.swem;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class SwemCompat {
    private static final String MOD_ID = "swem";
    private static boolean INSTALLED;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    @Nullable
    public static String getAnimation(Player player) {
        if (isInstalled()) {
            return SwemCompatInner.getAnimation(player);
        }
        return null;
    }

    public static void addBinding(CtrlBinding binding) {
        if (isInstalled()) {
            SwemCtrlBinding.addInnerBinding(binding);
        } else {
            addEmptyBinding(binding);
        }
    }

    /**
     * 没有安装此模组时，这些 molang 应该存在，否则会报错
     */
    private static void addEmptyBinding(CtrlBinding binding) {
        binding.playerVar("swem_is_ride", ctx -> false);
        binding.playerVar("swem_state", ctx -> StringUtils.EMPTY);
    }
}
