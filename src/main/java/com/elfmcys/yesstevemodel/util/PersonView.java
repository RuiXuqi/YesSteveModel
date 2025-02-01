package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class PersonView {
    public static int getPersonView(IContext<? extends Entity> ctx) {
        // 是客户端玩家，而且不在 GUI 渲染内
        if (ctx.entity() == Minecraft.getInstance().player && !isInInventory(ctx)) {
            return ctx.mc().options.getCameraType().ordinal();
        } else {
            // 否则永远返回第三人称正面视角
            return CameraType.THIRD_PERSON_FRONT.ordinal();
        }
    }

    public static boolean isInInventory(IContext<? extends Entity> ctx) {
        AnimatableEntity<?> animatableEntity = ctx.animatableEntity();
        return animatableEntity instanceof CustomGuiPlayerEntity || RenderUtil.isRenderingEntitiesInInventory();
    }
}
