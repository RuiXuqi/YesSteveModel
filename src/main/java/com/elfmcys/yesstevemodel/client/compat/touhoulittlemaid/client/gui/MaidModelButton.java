package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.gui;

import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.yesstevemodel.client.gui.button.ModelButton;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.network.message.YsmMaidModelMessage;

public class MaidModelButton extends ModelButton {
    private final EntityMaid maid;

    public MaidModelButton(int pX, int pY, boolean needAuth, CustomGuiPlayerEntity instance, ClientModel model, EntityMaid maid) {
        super(pX, pY, needAuth, instance, model);
        this.maid = maid;
    }

    @Override
    public void onPress() {
        if (needAuth) {
            return;
        }
        this.maid.setYsmModel(instance.getModelId(), instance.getTextureName());
        NetworkHandler.CHANNEL.sendToServer(new YsmMaidModelMessage(this.maid.getId(), instance.getModelId(), instance.getTextureName()));
    }
}
