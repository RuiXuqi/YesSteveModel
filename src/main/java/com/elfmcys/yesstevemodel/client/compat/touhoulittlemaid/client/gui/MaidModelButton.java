package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.gui;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.yesstevemodel.client.gui.button.ModelButton;
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
            cap.setYsmModel(animatedEntity.getModelId(), animatedEntity.getTextureName());
            // TODO: 重置 roaming 变量
            NetworkHandler.CHANNEL.sendToServer(new YsmMaidModelMessage(this.maid.getId(), animatedEntity.getModelId(), animatedEntity.getTextureName(), name));
        });
    }
}
