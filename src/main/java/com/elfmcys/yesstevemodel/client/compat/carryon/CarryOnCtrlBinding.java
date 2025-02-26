package com.elfmcys.yesstevemodel.client.compat.carryon;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public class CarryOnCtrlBinding {
    static void addInnerBinding(CtrlBinding binding) {
        binding.livingEntityVar("carryon_type", CarryOnCtrlBinding::getType);
        binding.livingEntityVar("carryon_is_princess", ctx -> CarryOnInnerCompat.isCarryOnPrincess(ctx.entity()));
    }

    private static String getType(IContext<LivingEntity> context) {
        if (context.entity() instanceof Player player) {
            CarryOnInnerCompat.Type type = CarryOnInnerCompat.getCarryOnType(player);
            if (type == CarryOnInnerCompat.Type.NONE) {
                return StringUtils.EMPTY;
            }
            return type.name().toLowerCase(Locale.ENGLISH);
        }
        return StringUtils.EMPTY;
    }
}
