package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.gui;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.yesstevemodel.client.gui.PlayerModelScreen;
import com.elfmcys.yesstevemodel.client.gui.PlayerTextureScreen;
import com.elfmcys.yesstevemodel.client.gui.button.ModelButton;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Objects;

public class MaidModelScreen extends PlayerModelScreen {
    private final EntityMaid maid;

    public MaidModelScreen(EntityMaid maid) {
        super();
        this.maid = maid;
    }

    @Override
    protected ModelButton getModelButton(int xStart, int yStart, boolean needAuth, CustomGuiPlayerEntity instance, ClientModel model) {
        return new MaidModelButton(xStart, yStart, needAuth, instance, model, maid);
    }

    @Override
    protected PlayerTextureScreen getTextureScreen(PlayerModelScreen parent, String modelId, ClientModel model) {
        ClientModel maidModel = this.maid.getCapability(YsmMaidCapabilityProvider.CAP).map(cap ->
                ClientModelManager.getModel(cap.getModelId()).get()).orElse(null);
        model = Objects.requireNonNullElse(maidModel, model);
        return new MaidTextureScreen(parent, modelId, model, maid);
    }

    @Override
    protected void renderReferenceEntity(GuiGraphics graphics, int mouseX, int mouseY) {
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int scissorX = (int) ((this.x + 5) * scale);
        int scissorY = (int) (window.getHeight() - ((this.y + 200) * scale));
        int scissorW = (int) (125 * scale);
        int scissorH = (int) (171 * scale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 67, y + 190, 70, x + 67 - mouseX, y + 180 - 95 - mouseY, this.maid);
        RenderSystem.disableScissor();

        this.maid.getCapability(YsmMaidCapabilityProvider.CAP).ifPresent(cap -> {
            String modelId = cap.getModelId();
            List<FormattedCharSequence> modelNameSplit = font.split(FormattedText.of(modelId), 125);
            int lineY = y + 205;
            for (FormattedCharSequence line : modelNameSplit) {
                int nameWidth = font.width(line);
                graphics.drawString(font, line, x + (135 - nameWidth) / 2, lineY, 0xF3EFE0);
                lineY += 10;
            }
        });
    }
}
