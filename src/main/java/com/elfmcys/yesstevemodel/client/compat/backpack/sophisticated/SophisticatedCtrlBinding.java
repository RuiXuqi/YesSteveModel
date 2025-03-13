package com.elfmcys.yesstevemodel.client.compat.backpack.sophisticated;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.compat.curios.CuriosCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

public class SophisticatedCtrlBinding {
    static void addInnerBinding(CtrlBinding binding) {
        binding.livingEntityVar("has_sophisticated_backpack", ctx -> hasSophisticatedBackpack(ctx.entity()));
    }

    private static boolean hasSophisticatedBackpack(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            final ItemStack[] backpack = new ItemStack[1];
            // 先尝试获取 curios 的背包
            if (CuriosCompat.isInstalled()) {
                backpack[0] = BackpackCuriosCompat.getCuriosBackpack(player);
            }
            // 然后才是玩家护甲栏的背包
            if (backpack[0] == null) {
                PlayerInventoryProvider.get().getBackpackFromRendered(player).ifPresent((backpackRenderInfo) -> {
                    backpack[0] = backpackRenderInfo.getBackpack();
                });
            }
            // 最终判定
            return backpack[0] != null;
        }
        return false;
    }
}
