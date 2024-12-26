package com.elfmcys.yesstevemodel.client.compat.carryon;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import net.minecraft.client.player.AbstractClientPlayer;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public class CarryOnCtrlBinding {
    static void addInnerBinding(CtrlBinding binding) {
        binding.playerVar("carryon_type", CarryOnCtrlBinding::getType);
        binding.playerVar("carryon_is_princess", ctx -> CarryOnInnerCompat.isCarryOnPrincess(ctx.entity()));
    }

    private static String getType(IContext<AbstractClientPlayer> context) {
        CarryOnInnerCompat.Type type = CarryOnInnerCompat.getCarryOnType(context.entity());
        if (type == CarryOnInnerCompat.Type.NONE) {
            return StringUtils.EMPTY;
        }
        return type.name().toLowerCase(Locale.ENGLISH);
    }
}
