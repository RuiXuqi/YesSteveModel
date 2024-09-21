package com.elfmcys.yesstevemodel.client.animation.condition;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

public class InnerClassify {
    private static final String EMPTY = "";

    public static String doClassifyTest(String extraPre, Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        Item item = itemInHand.getItem();
        String classify = getClassify(item);
        if (!classify.equals(EMPTY)) {
            return extraPre + classify;
        }
        return EMPTY;
    }

    public static String getClassify(Item item) {
        if (item instanceof SwordItem) {
            return "sword";
        }
        if (item instanceof AxeItem) {
            return "axe";
        }
        if (item instanceof PickaxeItem) {
            return "pickaxe";
        }
        if (item instanceof ShovelItem) {
            return "shovel";
        }
        if (item instanceof HoeItem) {
            return "hoe";
        }
        if (item instanceof ShieldItem) {
            return "shield";
        }
        if (item instanceof ThrowablePotionItem) {
            return "throwable_potion";
        }
        return EMPTY;
    }
}
