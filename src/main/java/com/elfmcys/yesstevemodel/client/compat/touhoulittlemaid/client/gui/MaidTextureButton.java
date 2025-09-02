package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.gui;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.yesstevemodel.client.gui.button.TextureButton;
import com.elfmcys.yesstevemodel.util.NameUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.network.message.YsmMaidModelMessage;
import net.minecraft.network.chat.Component;

public class MaidTextureButton extends TextureButton {
    private final EntityMaid renderMaid;
    private final int maidId;

    private String modelId;
    private String textureName;
    private Component name;

    public MaidTextureButton(int pX, int pY, CustomGuiPlayerEntity animatedEntity, EntityMaid rawMaid, int modelIndex, ClientModel clientModel) {
        super(pX, pY, animatedEntity, clientModel);
        this.renderMaid = new EntityMaid(rawMaid.level());
        this.renderMaid.setIsYsmModel(true);
        this.renderMaid.setOnGround(true);
        this.maidId = rawMaid.getId();
        rawMaid.getCapability(YsmMaidCapabilityProvider.CAP).ifPresent(cap -> {
            this.modelId = cap.getModelId();
            ClientModelManager.getModel(modelId).ifPresent(model -> {
                this.name = NameUtil.getModeName(model, modelId);
                this.textureName = model.textures().getKeyAt(modelIndex);
                this.renderMaid.setYsmModel(modelId, this.textureName, name);
                animatedEntity.setModelAndTexture(modelId, textureName);
            });
        });
    }

    @Override
    public void onPress() {
        this.renderMaid.setYsmModel(this.modelId, this.textureName, this.name);
        NetworkHandler.CHANNEL.sendToServer(new YsmMaidModelMessage(this.maidId, this.modelId, this.textureName, this.name));
    }
}
