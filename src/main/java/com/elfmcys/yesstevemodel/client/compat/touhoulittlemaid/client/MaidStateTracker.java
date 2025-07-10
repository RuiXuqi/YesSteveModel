package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client;

import com.elfmcys.yesstevemodel.api.IEntityExtraInfo;
import com.elfmcys.yesstevemodel.geckolib3.model.EntityStateTracker;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class MaidStateTracker extends EntityStateTracker<EntityMaid> implements IEntityExtraInfo {
    private ItemStack mainhandItemStack = ItemStack.EMPTY;
    private ItemStack offhandItemStack = ItemStack.EMPTY;

    public MaidStateTracker(EntityMaid entity) {
        super(entity);
    }

    public ItemStack getHandItem(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return mainhandItemStack;
        } else {
            return offhandItemStack;
        }
    }

    public void setHandItem(ItemStack stack, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            this.mainhandItemStack = stack;
        } else {
            this.offhandItemStack = stack;
        }
    }
}
