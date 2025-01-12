package com.elfmcys.yesstevemodel.client.compat.slashblade;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class SlashBladeCompat {
    private static final String SLASH_BLADE_ID = "slashblade";

    public static boolean isSlashBladeLoaded() {
        return ModList.get().isLoaded(SLASH_BLADE_ID);
    }

    public static boolean isSlashBladeItem(ItemStack stack) {
        return isSlashBladeLoaded() && stack.getItem() instanceof ItemSlashBlade;
    }

    public static String getAnimationName(AnimationEvent<CustomPlayerEntity> event) {
        if (isSlashBladeLoaded()) {
            return SlashBladeAnimation.getAnimationName(event);
        }
        return StringUtils.EMPTY;
    }

    @Nullable
    public static PlayState playMainAnimation(Player player, AnimationEvent<CustomPlayerEntity> event, String animationName, ILoopType loopType) {
        if (isSlashBladeLoaded() && isSlashBladeItem(player.getMainHandItem())) {
            return SlashBladeAnimation.playMainAnimation(event, animationName, loopType);
        }
        return null;
    }

    public static void addBinding(CtrlBinding binding) {
        if (isSlashBladeLoaded()) {
            SlashBladeBinding.addInnerBinding(binding);
        } else {
            addEmptyBinding(binding);
        }
    }

    /**
     * 没有安装此模组时，这些 molang 应该存在，否则会报错
     */
    private static void addEmptyBinding(CtrlBinding binding) {
        binding.playerVar("slashblade_animation", ctx -> StringUtils.EMPTY);
    }
}
