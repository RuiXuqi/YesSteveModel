package com.elfmcys.yesstevemodel.client.instance;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class YSMDebugSource implements DebugSource {
    public static final YSMDebugSource INSTANCE = new YSMDebugSource();

    private YSMDebugSource() {
    }

    @Override
    public void print(String message, Object...args) {
        if(Minecraft.getInstance().player == null) {
            return;
        }
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().player.sendSystemMessage(Component.translatable("message.yes_steve_model.model.debug_animation.output", String.format(message, args)));
        });
    }
}
