package com.elfmcys.yesstevemodel.api;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public interface IEntityExtraInfo {
    ItemStack getHandItem(InteractionHand hand);

    void setHandItem(ItemStack stack, InteractionHand hand);
}
