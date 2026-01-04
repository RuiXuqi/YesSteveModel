package com.elfmcys.yesstevemodel.client.compat.bettercombat.event;

import com.elfmcys.yesstevemodel.network.EmitSwingHand;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.client.BetterCombatClientEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

public class PlayerAttackEvent implements BetterCombatClientEvents.PlayerAttackStart {
    @Override
    public void onPlayerAttackStart(LocalPlayer player, AttackHand hand) {
        player.swingingArm = hand.isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        player.swingTime = -1;
        player.swinging = true;
        NetworkHandler.sendToServer(new EmitSwingHand(player.swingingArm));
    }
}
