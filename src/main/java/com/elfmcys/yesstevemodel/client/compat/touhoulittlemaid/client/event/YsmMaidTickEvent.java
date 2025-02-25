package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.event;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.roaming.RoamingStruct;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SubmitVariableChanges;
import com.github.tartaricacid.touhoulittlemaid.compat.ysm.event.YsmMaidClientTickEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ReferenceFloatPair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class YsmMaidTickEvent {
    @SubscribeEvent
    public void onTickYsmMaid(YsmMaidClientTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        EntityMaid maid = event.getMaid();
        if (player.getUUID().equals(maid.getOwnerUUID())) {
            submitRoamingVariableChanges(maid);
        }
    }

    private void submitRoamingVariableChanges(EntityMaid maid) {
        maid.getCapability(YsmMaidCapabilityProvider.CAP).ifPresent(cap -> {
            RoamingStruct roamingStruct = cap.getRemoteStruct();
            if (roamingStruct == null) {
                return;
            }
            if (!roamingStruct.isDirty()) {
                return;
            }
            var changes = roamingStruct.popChanges();
            List<ReferenceFloatPair<String>> variables = Lists.newArrayListWithCapacity(changes.variables.size());
            for (var entry : changes.variables.entrySet()) {
                variables.add(ReferenceFloatPair.of(entry.getKey(), entry.getValue()));
            }
            NetworkHandler.sendToServer(new SubmitVariableChanges(changes.instanceId, variables, maid.getId()));
        });
    }
}
