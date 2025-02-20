package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.gui;

import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.yesstevemodel.client.gui.PlayerModelScreen;
import com.elfmcys.yesstevemodel.client.gui.PlayerTextureScreen;
import com.elfmcys.yesstevemodel.client.gui.button.TextureButton;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public class MaidTextureScreen extends PlayerTextureScreen {
    private final EntityMaid maid;

    public MaidTextureScreen(PlayerModelScreen parent, String modelId, ClientModel model, EntityMaid maid) {
        super(parent, modelId, model);
        this.maid = maid;
    }

    @Override
    protected TextureButton getTextureButton(int pX, int pY, CustomGuiPlayerEntity instance, boolean disablePreviewRotation, int modelIndex) {
        return new MaidTextureButton(pX, pY, instance, disablePreviewRotation, maid, modelIndex);
    }

    @Override
    protected void renderReferenceEntity(GuiGraphics graphics, int scissorX, int scissorY, int scissorW, int scissorH) {
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, (int) (this.x + 299 / 2.0F + 40), (int) (this.y + 235 / 2.0F + 80), 80, 30, -10, this.maid);
        RenderSystem.disableScissor();
    }
}
