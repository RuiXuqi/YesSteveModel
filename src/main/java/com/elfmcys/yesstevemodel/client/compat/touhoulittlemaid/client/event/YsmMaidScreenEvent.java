package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.gui.MaidModelScreen;
import com.github.tartaricacid.touhoulittlemaid.compat.ysm.event.OpenYsmMaidScreenEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = YesSteveModel.MOD_ID)
public final class YsmMaidScreenEvent {
    @SubscribeEvent
    public void onOpenYsmMaidScreen(OpenYsmMaidScreenEvent event) {
        MaidModelScreen screen = new MaidModelScreen(event.getMaid());
        Minecraft.getInstance().setScreen(screen);
    }
}
