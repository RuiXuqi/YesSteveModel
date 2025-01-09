package com.elfmcys.yesstevemodel.client.compat.slashblade;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import mods.flammpfeil.slashblade.capability.slashblade.CapabilitySlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ComboState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class SlashBladeAnimation {
    static String getAnimationName(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatableEntity().getEntity();
        return getCombName(player.getMainHandItem(), player.level());
    }

    static String getAnimationName(IContext<AbstractClientPlayer> context) {
        AbstractClientPlayer player = context.entity();
        return getCombName(player.getMainHandItem(), player.level());
    }

    @NotNull
    private static String getCombName(ItemStack mainHandItem, Level level) {
        if (!(mainHandItem.getItem() instanceof ItemSlashBlade)) {
            return StringUtils.EMPTY;
        }
        return mainHandItem.getCapability(CapabilitySlashBlade.BLADESTATE).map(bladeState -> {
            long time = (level.getGameTime() - bladeState.getLastActionTime()) * 50;
            ComboState comboSeq = bladeState.getComboSeq();
            int timeout = comboSeq.getTimeoutMS();
            if (55 <= time && time <= timeout) {
                return "slashblade:" + comboSeq.getName().toLowerCase(Locale.ENGLISH);
            }
            return StringUtils.EMPTY;
        }).orElse(StringUtils.EMPTY);
    }
}
