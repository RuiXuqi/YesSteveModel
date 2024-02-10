package com.elfmcys.yesstevemodel.client.animation.condition;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

public class InnerClassify {
    private static final String EMPTY = "";

    public static String doClassifyTest(String extraPre, Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        Item item = itemInHand.getItem();
        if (item instanceof SwordItem) {
            return extraPre + "sword";
        }
        if (item instanceof AxeItem) {
            return extraPre + "axe";
        }
        if (item instanceof PickaxeItem) {
            return extraPre + "pickaxe";
        }
        if (item instanceof ShovelItem) {
            return extraPre + "shovel";
        }
        if (item instanceof HoeItem) {
            return extraPre + "hoe";
        }
        if (item instanceof ShieldItem) {
            return extraPre + "shield";
        }
        if (item instanceof ThrowablePotionItem) {
            return extraPre + "throwable_potion";
        }
        return EMPTY;
    }
}
