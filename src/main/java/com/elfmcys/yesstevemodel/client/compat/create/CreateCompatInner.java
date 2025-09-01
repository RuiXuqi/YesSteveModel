package com.elfmcys.yesstevemodel.client.compat.create;

import com.elfmcys.yesstevemodel.mixin.client.create.PlayerSkyhookRendererAccessor;
import net.minecraft.world.entity.player.Player;

public class CreateCompatInner {
    static boolean isHangingSkyhook(Player player) {
        return PlayerSkyhookRendererAccessor.getHangingPlayers().contains(player.getUUID());
    }
}
