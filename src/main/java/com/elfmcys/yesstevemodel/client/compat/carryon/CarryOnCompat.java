package com.elfmcys.yesstevemodel.client.compat.carryon;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.function.BiFunction;

public class CarryOnCompat {
    private static final String CARRY_ON_ID = "carryon";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(CARRY_ON_ID);
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static Optional<BiFunction<String, CustomPlayerEntity, IAnimationController<CustomPlayerEntity>>> animationPredicate() {
        if (INSTALLED) {
            return Optional.of((name, entity) -> new HybridAnimationController<>(entity, name, 0.1f, new CarryOnPredicate()));
        } else {
            return Optional.empty();
        }
    }

    public static boolean isCarryOnPrincess(Player player) {
        if (INSTALLED) {
            return CarryOnInnerCompat.isCarryOnPrincess(player);
        }
        return false;
    }

    public static void addBinding(CtrlBinding binding) {
        if (isInstalled()) {
            CarryOnCtrlBinding.addInnerBinding(binding);
        } else {
            addEmptyBinding(binding);
        }
    }

    /**
     * 没有安装此模组时，这些 molang 应该存在，否则会报错
     */
    private static void addEmptyBinding(CtrlBinding binding) {
        binding.livingEntityVar("carryon_type", ctx -> StringUtils.EMPTY);
        binding.livingEntityVar("carryon_is_princess", ctx -> false);
    }
}
