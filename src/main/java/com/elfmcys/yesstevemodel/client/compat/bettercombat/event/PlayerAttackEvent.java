package com.elfmcys.yesstevemodel.client.compat.bettercombat.event;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.client.BetterCombatClientEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

public class PlayerAttackEvent implements BetterCombatClientEvents.PlayerAttackStart {
    @Override
    public void onPlayerAttackStart(LocalPlayer player, AttackHand hand) {
        player.swing(hand.isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }
}
