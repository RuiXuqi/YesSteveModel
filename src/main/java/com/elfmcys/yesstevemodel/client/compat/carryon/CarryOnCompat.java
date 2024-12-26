package com.elfmcys.yesstevemodel.client.compat.carryon;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

public class CarryOnCompat {
    private static final String CARRY_ON_ID = "carryon";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(CARRY_ON_ID);
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static PlayState predicateCarryOn(AnimationEvent<CustomPlayerEntity> event) {
        if (INSTALLED) {
            return CarryOnInnerCompat.predicateCarryOn(event);
        }
        return PlayState.STOP;
    }

    public static boolean isCarryOnPrincess(Player player, AnimationEvent<CustomPlayerEntity> event) {
        if (INSTALLED) {
            return CarryOnInnerCompat.isCarryOnPrincess(player);
        }
        return false;
    }

    public static void addBinding(CtrlBinding binding) {
        if (isInstalled()) {
            CarryOnCtrlBinding.addInnerBinding(binding);
        }
    }
}
