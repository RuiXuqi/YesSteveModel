package com.elfmcys.yesstevemodel.client.compat.backpack.sophisticated;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;

public class SophisticatedCtrlBinding {
    static void addInnerBinding(CtrlBinding binding) {
        binding.livingEntityVar("has_sophisticated_backpack", ctx ->
                SophisticatedCompat.getBackpackItemStack(ctx.entity()) != null);
    }
}
