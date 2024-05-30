package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.gui.button.AuthorButton;
import com.elfmcys.yesstevemodel.client.gui.button.FlatColorButton;
import com.elfmcys.yesstevemodel.info.ModelAuthor;
import com.elfmcys.yesstevemodel.info.ModelInfo;
import com.elfmcys.yesstevemodel.info.ModelMetadata;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ModelInfoScreen extends Screen {
    private final static ResourceLocation DEFAULT_AVATAR = new ResourceLocation(YesSteveModel.MOD_ID, "texture/default_avatar.png");
    private final PlayerModelScreen parent;
    private final ClientModel model;
    private final ModelInfo modelInfo;
    private int x;
    private int y;

    public ModelInfoScreen(PlayerModelScreen parent, ClientModel model) {
        super(Component.literal("Model Info GUI"));
        this.parent = parent;
        this.model = model;
        this.modelInfo = model.modelInfo();
    }

    @Override
    protected void init() {
        this.clearWidgets();

        this.x = (width - 420) / 2;
        this.y = (height - 235) / 2;

        ModelMetadata metadata = this.modelInfo.metadata();
        if (metadata == null) {
            return;
        }

        for (int i = 0; i < 5; i++) {
            List<ModelAuthor> authors = metadata.authors();
            if (i >= authors.size()) {
                for (; i < 5; i++) {
                    addRenderableWidget(AuthorButton.empty(this.x + 25 + 75 * i, this.y + 15));
                }
                continue;
            }
            ModelAuthor author = authors.get(i);
            ResourceLocation avatar = model.clientModelInfo().authorAvatars().getOrDefault(author.name(), DEFAULT_AVATAR);
            addRenderableWidget(new AuthorButton(this.x + 25 + 75 * i, this.y + 15, author, avatar));
        }

        addRenderableWidget(new FlatColorButton(x + 2, y + 25, 18, 100, Component.literal("<"), (b) -> {
        }).setTooltips("gui.yes_steve_model.pre_page"));
        addRenderableWidget(new FlatColorButton(x + 25 + 75 * 5, y + 25, 18, 100, Component.literal(">"), (b) -> {
        }).setTooltips("gui.yes_steve_model.next_page"));
        addRenderableWidget(new FlatColorButton(this.x + 310, this.y + 150, 85, 20, Component.translatable("gui.yes_steve_model.url.home"), (b) -> {
            openUrl(metadata.links().get("home"));
        }));
        addRenderableWidget(new FlatColorButton(this.x + 310, this.y + 175, 85, 20, Component.translatable("gui.yes_steve_model.url.donate"), (b) -> {
            openUrl(metadata.links().get("donate"));
        }));
        addRenderableWidget(new FlatColorButton(this.x + 310, this.y + 200, 85, 20, Component.translatable("gui.yes_steve_model.model.return"), (b) -> {
            this.getMinecraft().setScreen(parent);
        }));
    }

    private void openUrl(String homeUrl) {
        if (StringUtils.isNoneBlank(homeUrl)) {
            this.getMinecraft().setScreen(new ConfirmLinkScreen(yes -> {
                if (yes) {
                    Util.getPlatform().openUri(homeUrl);
                }
                this.getMinecraft().setScreen(this);
            }, homeUrl, true));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        graphics.fillGradient(this.x + 25, this.y + 150, this.x + 305, this.y + 220, 0x8F_5B5B5B, 0x8F_5B5B5B);

        ModelMetadata metadata = this.modelInfo.metadata();
        if (metadata != null) {
            String tips = metadata.tips();
            graphics.drawWordWrap(font, Component.literal(tips), this.x + 30, this.y + 155, 270, 0xFFFFFF);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
