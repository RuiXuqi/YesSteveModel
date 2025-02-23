package com.elfmcys.yesstevemodel.client.compat.backpack.sophisticated;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class BackpackCuriosCompat {
    private static final String SLOT_TYPE = "back";

    @Nullable
    static ItemStack getCuriosBackpack(AbstractClientPlayer player) {
        final ItemStack[] backpack = new ItemStack[1];
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getStacksHandler(SLOT_TYPE).ifPresent(stacksHandler -> {
            IDynamicStackHandler stacks = stacksHandler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack itemStack = stacks.getStackInSlot(i);
                if (itemStack.getItem() instanceof BackpackItem) {
                    backpack[0] = itemStack;
                    return;
                }
            }
        }));
        return backpack[0];
    }
}
