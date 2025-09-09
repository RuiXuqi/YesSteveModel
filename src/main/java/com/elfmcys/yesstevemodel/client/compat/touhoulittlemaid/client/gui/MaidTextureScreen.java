package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.gui;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.client.event.RegisterEntityRenderersEvent;
import com.elfmcys.yesstevemodel.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.yesstevemodel.client.gui.PlayerModelScreen;
import com.elfmcys.yesstevemodel.client.gui.PlayerTextureScreen;
import com.elfmcys.yesstevemodel.client.gui.button.TextureButton;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

public class MaidTextureScreen extends PlayerTextureScreen {
    private final EntityMaid maid;

    public MaidTextureScreen(PlayerModelScreen parent, String modelId, ClientModel model, EntityMaid maid) {
        super(parent, modelId, model);
        this.maid = maid;
    }

    @Override
    protected TextureButton getTextureButton(int pX, int pY, CustomGuiPlayerEntity animatedEntity, int modelIndex) {
        return new MaidTextureButton(pX, pY, animatedEntity, maid, modelIndex, model);
    }

    @Override
    protected void renderReferenceEntity(GuiGraphics graphics, int scissorX, int scissorY, int scissorW, int scissorH, float partialTicks) {
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
        maid.getCapability(YsmMaidCapabilityProvider.CAP).ifPresent(cap -> {
            previewEntity.updateModelAndTexture(cap.getModelId(), cap.getTextureName());
            RenderUtil.renderTextureScreenEntity(this.x + 299 / 2.0F + 40 + posX, this.y + 235 / 2.0F + 80 + posY, scale, pitch, yaw, partialTicks, previewEntity, RegisterEntityRenderersEvent.getPlayerRenderer(), showGround);
        });
        RenderSystem.disableScissor();
    }
}
