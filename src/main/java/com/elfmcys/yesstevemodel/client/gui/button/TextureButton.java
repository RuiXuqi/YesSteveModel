package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.gui.GuiModelInstance;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.roaming.RoamingStruct;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SetModelAndTexture;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
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
    private final String name;
    private final GuiModelInstance instance;

    public TextureButton(int pX, int pY, GuiModelInstance instance) {
        super(pX, pY, 54, 102, Component.empty(), (b) -> {
        }, DEFAULT_NARRATION);
        this.name = ModelIdUtil.getSubNameFromId(instance.getTextureLocation());
        this.instance = instance;
    }

    @Override
    public void onPress() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
                cap.setTexture(instance.getTextureLocation());
                if (cap.getAnimatable().getRemoteStruct() instanceof RoamingStruct roamingStruct) {
                    NetworkHandler.sendToServer(new SetModelAndTexture(instance.getModelId(), instance.getTextureLocation(), roamingStruct.getInstanceId()));
                }
            });
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF_434242, 0xFF_434242);
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int scissorX = (int) (this.getX() * scale);
        int scissorY = (int) (window.getHeight() - ((this.getY() + this.height - 20) * scale));
        int scissorW = (int) (this.width * scale);
        int scissorH = (int) ((this.height - 20) * scale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
        RenderUtil.renderModelInInventory(this.getX() + this.width / 2, this.getY() + this.height / 2 + 24, 35, instance);
        RenderSystem.disableScissor();

        Component message = Component.literal(name);
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
}
