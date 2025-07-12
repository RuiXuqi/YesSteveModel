package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.gui.MaidModelScreen;
import com.github.tartaricacid.touhoulittlemaid.compat.ysm.event.OpenYsmMaidScreenEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class YsmMaidScreenEvent {
    @SubscribeEvent
    public void onOpenYsmMaidScreen(OpenYsmMaidScreenEvent event) {
        if (!YesSteveModel.isAvailable()) {
            YesSteveModel.sendUnavailableMessage();
            return;
        }
        MaidModelScreen screen = new MaidModelScreen(event.getMaid());
        Minecraft.getInstance().setScreen(screen);
    }
}
