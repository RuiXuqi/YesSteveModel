package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.event;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.github.tartaricacid.touhoulittlemaid.compat.ysm.event.UpdateRemoteStructEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class UpdateRemoteStruct {
    @SubscribeEvent
    public void onUpdateRemote(UpdateRemoteStructEvent event) {
        event.getMaid().getCapability(YsmMaidCapabilityProvider.CAP).ifPresent(cap -> cap.setRemoteStruct(event.getRoamingVars()));
    }
}
