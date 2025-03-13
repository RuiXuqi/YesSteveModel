package com.elfmcys.yesstevemodel.client.compat.backpack.sophisticated;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class BackpackCuriosCompat {
    private static final String SLOT_TYPE = "back";

    @Nullable
    static ItemStack getCuriosBackpack(Player player) {
        final ItemStack[] backpack = new ItemStack[1];
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getStacksHandler(SLOT_TYPE).ifPresent(stacksHandler -> {
            IDynamicStackHandler stacks = stacksHandler.getStacks();
            NonNullList<Boolean> renders = stacksHandler.getRenders();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack itemStack = stacks.getStackInSlot(i);
                if (itemStack.getItem() instanceof BackpackItem) {
                    // 检查是否显示
                    if (i < renders.size() && renders.get(i)) {
                        backpack[0] = itemStack;
                    }
                    return;
                }
            }
        }));
        return backpack[0];
    }
}
