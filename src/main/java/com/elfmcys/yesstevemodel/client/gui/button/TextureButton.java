package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SetModelAndTexture;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class TextureButton extends Button {
    protected final CustomGuiPlayerEntity animatedEntity;
    protected final boolean disablePreviewRotation;

    public TextureButton(int pX, int pY, CustomGuiPlayerEntity animatedEntity, boolean disablePreviewRotation) {
        super(pX, pY, 54, 102, Component.empty(), (b) -> {
        }, DEFAULT_NARRATION);
        this.animatedEntity = animatedEntity;
        this.disablePreviewRotation = disablePreviewRotation;
    }

    @Override
    public void onPress() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                cap.setTextureName(animatedEntity.getTextureName());
                NetworkHandler.sendToServer(new SetModelAndTexture(animatedEntity.getModelId(), animatedEntity.getTextureName()));
            });
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF_434242, 0xFF_434242);
        renderReferenceEntity(graphics);

        Component message = Component.literal(animatedEntity.getTextureName());
        List<FormattedCharSequence> split = font.split(message, 50);
        if (split.size() > 1) {
            graphics.drawCenteredString(font, split.get(0), this.getX() + this.width / 2, this.getY() + this.height - 19, 0xF3EFE0);
            graphics.drawCenteredString(font, split.get(1), this.getX() + this.width / 2, this.getY() + this.height - 10, 0xF3EFE0);
        } else {
            graphics.drawCenteredString(font, message, this.getX() + this.width / 2, this.getY() + this.height - 15, 0xF3EFE0);
        }
        if (this.isHoveredOrFocused()) {
            graphics.fillGradient(this.getX(), this.getY() + 1, this.getX() + 1, this.getY() + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX() + this.width - 1, this.getY() + 1, this.getX() + this.width, this.getY() + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, 0xff_F3EFE0, 0xff_F3EFE0);
        }
    }

    protected void renderReferenceEntity(GuiGraphics graphics) {
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int scissorX = (int) (this.getX() * scale);
        int scissorY = (int) (window.getHeight() - ((this.getY() + this.height - 20) * scale));
        int scissorW = (int) (this.width * scale);
        int scissorH = (int) ((this.height - 20) * scale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
        RenderUtil.renderModelInInventory(this.getX() + this.width / 2, this.getY() + this.height / 2 + 24, 35, animatedEntity, this.disablePreviewRotation);
        RenderSystem.disableScissor();
    }
}
