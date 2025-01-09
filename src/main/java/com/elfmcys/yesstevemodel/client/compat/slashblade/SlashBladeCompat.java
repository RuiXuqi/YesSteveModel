package com.elfmcys.yesstevemodel.client.compat.slashblade;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.StringUtils;

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

    public static void addBinding(CtrlBinding binding) {
        if (isSlashBladeLoaded()) {
            SlashBladeBinding.addInnerBinding(binding);
        }
    }
}
