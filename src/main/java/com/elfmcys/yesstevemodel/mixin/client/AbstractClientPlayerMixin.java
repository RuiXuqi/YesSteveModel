package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.api.IPlayerExtraInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin implements IPlayerExtraInfo {
    @Unique
    private ItemStack mainhandItemStack = ItemStack.EMPTY;

    @Unique
    private ItemStack offhandItemStack = ItemStack.EMPTY;


    @Override
    @Unique
    public ItemStack getHandItem(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return mainhandItemStack;
        }
        return offhandItemStack;
    }

    @Override
    @Unique
    public void setHandItem(ItemStack stack, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            this.mainhandItemStack = stack;
        } else {
            this.offhandItemStack = stack;
        }
    }
}
