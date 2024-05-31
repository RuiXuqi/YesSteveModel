package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.info.ModelAuthor;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class AuthorButton extends Button {
    private final ModelAuthor author;
    private final ResourceLocation avatar;
    private final List<Component> tooltips;

    public AuthorButton(int pX, int pY, ModelAuthor author, ResourceLocation avatar) {
        super(pX, pY, 70, 130, Component.empty(), (b) -> {
        }, DEFAULT_NARRATION);
        this.author = author;
        this.avatar = avatar;
        this.tooltips = Lists.newArrayList();
        if (this.author != null) {
            this.author.contact().forEach((k, v) -> {
                this.tooltips.add(Component.literal(k + ": " + v));
            });
        }
    }

    public static AuthorButton empty(int pX, int pY) {
        return new AuthorButton(pX, pY, null, null);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        if (author == null || avatar == null) {
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x8F_434242, 0x8F_434242);
            graphics.drawCenteredString(font, Component.literal("......"), this.getX() + this.width / 2, this.getY() + this.height / 2, ChatFormatting.GRAY.getColor());
            return;
        }
        if (this.isHoveredOrFocused()) {
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x8F_306BAC, 0x8F_306BAC);
        } else {
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x8F_434242, 0x8F_434242);
        }
        graphics.blit(avatar, this.getX() + 3, this.getY() + 3, 64, 64, 0, 0, 64, 64, 64, 64);
        renderScrollingString(graphics, font, Component.literal(author.name()), this.getX() + 2, this.getY() + 72, this.getX() + width - 2, this.getY() + 82, ChatFormatting.GOLD.getColor());
        graphics.drawCenteredString(font, author.role(), this.getX() + 35, this.getY() + 82, ChatFormatting.GREEN.getColor());
        graphics.drawWordWrap(font, Component.literal(author.comment()), this.getX() + 3, this.getY() + 95, 64, 0xFFFFFFFF);
    }

    public void renderToolTip(GuiGraphics graphics, Screen screen, int pMouseX, int pMouseY) {
        if (this.isHovered && !tooltips.isEmpty()) {
            graphics.renderComponentTooltip(screen.getMinecraft().font, tooltips, pMouseX, pMouseY);
        }
    }
}
