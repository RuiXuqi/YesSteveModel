package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.StarModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.client.gui.GuiModelInstance;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.roaming.RoamingStruct;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SetModelAndTexture;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class ModelButton extends Button {
    private final static ResourceLocation ICON = new ResourceLocation(YesSteveModel.MOD_ID, "texture/icon.png");
    private final boolean needAuth;
    private final int color;
    private final List<Component> tooltips;
    private final GuiModelInstance instance;

    public ModelButton(int pX, int pY, boolean needAuth, GuiModelInstance instance, List<Component> tooltips) {
        super(pX, pY, 52, 90, Component.literal(instance.getModelId().getPath()), (b) -> {
        }, DEFAULT_NARRATION);
        this.needAuth = needAuth;
        this.color = needAuth ? 0x7F_000000 : 0xFF_434242;
        this.tooltips = tooltips;
        this.instance = instance;
    }

    @Override
    public void onPress() {
        if (needAuth) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
                cap.setModelAndTexture(instance.getModelId(), instance.getTextureLocation());
                if (cap.getAnimatable().getRemoteStruct() instanceof RoamingStruct roamingStruct) {
                    roamingStruct.reset(roamingStruct.getInstanceId() + 1, null);
                    NetworkHandler.sendToServer(new SetModelAndTexture(instance.getModelId(), instance.getTextureLocation(), roamingStruct.getInstanceId()));
                }
            });
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, this.color, this.color);
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int scissorX = (int) (this.getX() * scale);
        int scissorY = (int) (window.getHeight() - ((this.getY() + this.height - 20) * scale));
        int scissorW = (int) (this.width * scale);
        int scissorH = (int) ((this.height - 20) * scale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
        RenderUtil.renderModelInInventory(this.getX() + this.width / 2, this.getY() + this.height / 2 + 20, 30, instance);
        RenderSystem.disableScissor();

        Component message = this.getMessage();
        List<FormattedCharSequence> split = font.split(message, 45);
        if (split.size() > 1) {
            graphics.drawCenteredString(font, split.get(0), this.getX() + this.width / 2, this.getY() + this.height - 19, 0xF3EFE0);
            graphics.drawCenteredString(font, split.get(1), this.getX() + this.width / 2, this.getY() + this.height - 10, 0xF3EFE0);
        } else {
            graphics.drawCenteredString(font, this.getMessage(), this.getX() + this.width / 2, this.getY() + this.height - 15, 0xF3EFE0);
        }
        if (!this.needAuth && this.isHoveredOrFocused()) {
            graphics.fillGradient(this.getX(), this.getY() + 1, this.getX() + 1, this.getY() + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX() + this.width - 1, this.getY() + 1, this.getX() + this.width, this.getY() + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, 0xff_F3EFE0, 0xff_F3EFE0);
        }

        if (minecraft.player != null) {
            minecraft.player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).ifPresent(cap -> {
                if (cap.containModel(instance.getModelId())) {
                    graphics.blit(ICON, this.getX() + this.width - 14, this.getY(), 16, 16, 16, 0, 16, 16, 256, 256);
                }
            });
        }

        if (needAuth) {
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x9f_222222, 0x9f_222222);
        }
    }

    public void renderComponentTooltip(GuiGraphics graphics, Screen screen, int pMouseX, int pMouseY) {
        if (this.isHovered() && tooltips != null) {
            graphics.renderComponentTooltip(screen.getMinecraft().font, tooltips, pMouseX, pMouseY);
        }
    }


    @Override
    protected boolean clicked(double pMouseX, double pMouseY) {
        return !this.needAuth && super.clicked(pMouseX, pMouseY);
    }
}
