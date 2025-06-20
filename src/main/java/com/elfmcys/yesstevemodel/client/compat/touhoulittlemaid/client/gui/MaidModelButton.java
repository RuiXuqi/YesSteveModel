package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.gui;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.yesstevemodel.client.gui.button.ModelButton;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.roaming.RoamingStruct;
import com.elfmcys.yesstevemodel.util.NameUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.network.message.YsmMaidModelMessage;
import net.minecraft.network.chat.Component;

public class MaidModelButton extends ModelButton {
    private final EntityMaid maid;

    public MaidModelButton(int pX, int pY, boolean needAuth, CustomGuiPlayerEntity animatedEntity, ClientModel model, EntityMaid maid) {
        super(pX, pY, needAuth, animatedEntity, model);
        this.maid = maid;
    }

    @Override
    public void onPress() {
        if (needAuth) {
            return;
        }
        Component name = NameUtil.getModeName(model, animatedEntity.getModelId());
        this.maid.getCapability(YsmMaidCapabilityProvider.CAP).ifPresent(cap -> {
            var oldModelId = cap.getModelId();
            cap.setYsmModel(animatedEntity.getModelId(), animatedEntity.getTextureName());
            RoamingStruct remoteStruct = cap.getRemoteStruct();
            if (!oldModelId.equals(animatedEntity.getModelId())) {
                remoteStruct.reset(remoteStruct.getInstanceId() + 1, null);
            }
            NetworkHandler.CHANNEL.sendToServer(new YsmMaidModelMessage(this.maid.getId(), animatedEntity.getModelId(), animatedEntity.getTextureName(), name));
        });
    }
}
