package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.gui;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.yesstevemodel.client.gui.button.TextureButton;
import com.elfmcys.yesstevemodel.util.NameUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.network.message.YsmMaidModelMessage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public class MaidTextureButton extends TextureButton {
    private final EntityMaid renderMaid;
    private final int maidId;

    private String modelId;
    private String textureName;
    private Component name;

    public MaidTextureButton(int pX, int pY, CustomGuiPlayerEntity animatedEntity, boolean disablePreviewRotation, EntityMaid rawMaid, int modelIndex) {
        super(pX, pY, animatedEntity, disablePreviewRotation);
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
            });
        });
    }

    @Override
    public void onPress() {
        this.renderMaid.setYsmModel(this.modelId, this.textureName, this.name);
        NetworkHandler.CHANNEL.sendToServer(new YsmMaidModelMessage(this.maidId, this.modelId, this.textureName, this.name));
    }

    @Override
    protected void renderReferenceEntity(GuiGraphics graphics) {
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int scissorX = (int) (this.getX() * scale);
        int scissorY = (int) (window.getHeight() - ((this.getY() + this.height - 20) * scale));
        int scissorW = (int) (this.width * scale);
        int scissorH = (int) ((this.height - 20) * scale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                this.getX() + this.width / 2, this.getY() + this.height / 2 + 24, 35,
                30, -10, this.renderMaid);
        RenderSystem.disableScissor();
    }
}
