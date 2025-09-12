package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.client.gui.button.AuthorButton;
import com.elfmcys.yesstevemodel.client.gui.button.FlatColorButton;
import com.elfmcys.yesstevemodel.client.lang.LanguageManager;
import com.elfmcys.yesstevemodel.info.ModelAuthor;
import com.elfmcys.yesstevemodel.info.ModelInfo;
import com.elfmcys.yesstevemodel.info.ModelMetadata;
import com.google.common.collect.ImmutableMap;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
public class ModelInfoScreen extends Screen {
    private final static ResourceLocation DEFAULT_AVATAR = new ResourceLocation(YesSteveModel.MOD_ID, "texture/default_avatar.png");
    private final static Map<String, Component> LINK_TYPE_PRESET = ImmutableMap.of(
            "home", Component.translatable("gui.yes_steve_model.url.home"),
            "donate", Component.translatable("gui.yes_steve_model.url.donate")
    );

    private final PlayerModelScreen parent;
    private final ClientModel model;
    private final ModelInfo modelInfo;
    private int startAuthorIndex = 0;
    private int x;
    private int y;

    public ModelInfoScreen(PlayerModelScreen parent, ClientModel model) {
        super(Component.literal("Model Info GUI"));
        this.parent = parent;
        this.model = model;
        this.modelInfo = model.info();
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
        List<ModelAuthor> authors = metadata.authors();
        if (authors.size() <= startAuthorIndex) {
            startAuthorIndex = 0;
        }

        for (int i = 0; i < 5; i++) {
            int index = startAuthorIndex + i;
            if (index >= authors.size()) {
                for (; i < 5; i++) {
                    addRenderableWidget(AuthorButton.empty(this.x + 25 + 75 * i, this.y + 15, this));
                }
                continue;
            }
            ModelAuthor author = authors.get(index);
            ResourceLocation avatar = model.clientInfo().authorAvatars().getOrDefault(author.name(), DEFAULT_AVATAR);
            addRenderableWidget(new AuthorButton(this.x + 25 + 75 * i, this.y + 15, author, model, avatar, index, this));
        }

        addRenderableWidget(new FlatColorButton(x + 2, y + 25, 18, 100, Component.literal("<"), (b) -> {
            startAuthorIndex = Math.max(0, startAuthorIndex - 5);
            this.init();
        }).setTooltips("gui.yes_steve_model.pre_page"));
        addRenderableWidget(new FlatColorButton(x + 25 + 75 * 5, y + 25, 18, 100, Component.literal(">"), (b) -> {
            startAuthorIndex = startAuthorIndex + 5;
            this.init();
        }).setTooltips("gui.yes_steve_model.next_page"));

        int y = this.y + 150;
        for (var i = 0; i < Math.min(metadata.links().size(), 2); i++) {
            var type = metadata.links().getKeyAt(i);
            var value = metadata.links().getValueAt(i);

            var displayText = LINK_TYPE_PRESET.get(type);
            if (displayText == null) {
                displayText = Component.literal(type);
            }

            addRenderableWidget(new FlatColorButton(this.x + 310, y, 85, 20, displayText, b -> openUrl(value)));
            y += 25;
        }
        addRenderableWidget(new FlatColorButton(this.x + 310, y, 85, 20, Component.translatable("gui.yes_steve_model.model.return"), (b) -> {
            this.getMinecraft().setScreen(parent);
        }));
    }

    private void openUrl(@Nullable String homeUrl) {
        if (homeUrl != null && StringUtils.isNoneBlank(homeUrl)) {
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
            String tips = LanguageManager.getI18n(model, "metadata.tips", metadata.tips());
            List<FormattedCharSequence> splitDesc = font.split(Component.literal(tips), 270);
            int offset = 0;
            for (FormattedCharSequence desc : splitDesc) {
                graphics.drawString(font, desc, this.x + 30, this.y + 154 + offset, 0xFFFFFFFF);
                offset += font.lineHeight;
                if (offset > font.lineHeight * 7) {
                    break;
                }
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);

        this.renderables.stream().filter(r -> r instanceof AuthorButton)
                .forEach(r -> ((AuthorButton) r).renderToolTip(graphics, this, mouseX, mouseY));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
