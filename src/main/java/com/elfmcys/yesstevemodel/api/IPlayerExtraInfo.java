package com.elfmcys.yesstevemodel.api;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public interface IPlayerExtraInfo {
    ItemStack getHandItem(InteractionHand hand);

    void setHandItem(ItemStack stack, InteractionHand hand);
}
