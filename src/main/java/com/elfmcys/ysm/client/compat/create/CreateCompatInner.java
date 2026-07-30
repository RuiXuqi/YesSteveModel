package com.elfmcys.ysm.client.compat.create;

import com.elfmcys.ysm.mixin.client.create.PlayerSkyhookRendererAccessor;
import net.minecraft.world.entity.player.Player;

public class CreateCompatInner {
    static boolean isHangingSkyhook(Player player) {
        return PlayerSkyhookRendererAccessor.getHangingPlayers().contains(player.getUUID());
    }
}
