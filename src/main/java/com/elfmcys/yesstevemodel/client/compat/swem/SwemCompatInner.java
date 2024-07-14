package com.elfmcys.yesstevemodel.client.compat.swem;

import com.alaharranhonor.swem.forge.entities.horse.SWEMHorseEntityBase;
import com.google.common.collect.Maps;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;

public class SwemCompatInner {
    private static final EnumMap<SWEMHorseEntityBase.Gait, String> CACHE = Maps.newEnumMap(SWEMHorseEntityBase.Gait.class);

    @Nullable
    @SuppressWarnings("all")
    static String getAnimation(Player player) {
        if (player.getVehicle() instanceof SWEMHorseEntityBase horse) {
            SWEMHorseEntityBase.Gait gait = horse.getGait();
            return CACHE.computeIfAbsent(gait, g -> "swem:" + g.name().toLowerCase(Locale.ENGLISH));
        }
        return null;
    }
}
