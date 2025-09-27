package com.elfmcys.yesstevemodel.client.compat.backpack.sophisticated;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.compat.curios.CuriosCompat;
import com.elfmcys.yesstevemodel.client.event.RegisterEntityRenderersEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.LoadingModList;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import org.jetbrains.annotations.Nullable;

public class SophisticatedCompat {
    private static final String MOD_ID = "sophisticatedbackpacks";
    private static boolean INSTALLED;

    /**
     * 需要在资源加载后初始化
     */
    public static void init() {
        INSTALLED = LoadingModList.get().getModFileById(MOD_ID) != null;
    }

    public static void addLayer() {
        // 以防加载顺序的不同，导致没有初始化
        INSTALLED = LoadingModList.get().getModFileById(MOD_ID) != null;
        if (isInstalled()) {
            RegisterEntityRenderersEvent.getPlayerRenderer().addLayer(new YsmBackpackLayerRenderer());
        }
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static void addBinding(CtrlBinding binding) {
        if (isInstalled()) {
            SophisticatedCtrlBinding.addInnerBinding(binding);
        } else {
            addEmptyBinding(binding);
        }
    }

    /**
     * 没有安装此模组时，这些 molang 应该存在，否则会报错
     */
    private static void addEmptyBinding(CtrlBinding binding) {
        binding.livingEntityVar("has_sophisticated_backpack", ctx -> false);
    }

    @Nullable
    public static ItemStack getBackpackItemStack(LivingEntity livingEntity) {
        // 先尝试获取 curios 的背包
        if (CuriosCompat.isInstalled()) {
            var backpack = BackpackCuriosCompat.getCuriosBackpack(livingEntity);
            if (backpack != null) {
                return backpack;
            }
        }
        // 然后才是玩家护甲栏的背包
        if (livingEntity instanceof Player player) {
            return PlayerInventoryProvider.get().getBackpackFromRendered(player)
                    .map(backpackRenderInfo -> {
                        var itemStack = backpackRenderInfo.getBackpack();
                        if (!itemStack.isEmpty()) {
                            return itemStack;
                        }
                        return null;
                    })
                    .orElse(null);
        }
        return null;
    }
}
