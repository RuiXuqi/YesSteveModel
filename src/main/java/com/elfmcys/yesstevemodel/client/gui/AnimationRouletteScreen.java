package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.animation.molang.CustomMolangParser;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.gui.button.FlatCheckbox;
import com.elfmcys.yesstevemodel.client.gui.button.FlatColorButton;
import com.elfmcys.yesstevemodel.client.gui.button.FlatRatioBox;
import com.elfmcys.yesstevemodel.client.gui.button.FlatSlider;
import com.elfmcys.yesstevemodel.client.input.ExtraAnimationKey;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.info.ModelProperties;
import com.elfmcys.yesstevemodel.info.roulette.ExtraAnimationButton;
import com.elfmcys.yesstevemodel.info.roulette.forms.CheckboxForms;
import com.elfmcys.yesstevemodel.info.roulette.forms.ConfigForms;
import com.elfmcys.yesstevemodel.info.roulette.forms.RadioForms;
import com.elfmcys.yesstevemodel.info.roulette.forms.RangeForms;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SetPlayAnimation;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;

public class AnimationRouletteScreen extends Screen {
    private static final String SPEC_PREFIX = "#";
    private static final int MAX_ROULETTE_COUNT = 8;
    /**
     * 用来缓存当前页面的打开情况，用于在每次打开时，都能记住上一次的页数
     */
    private static final LinkedList<Pair<String, Integer>> CACHE = Lists.newLinkedList();
    /**
     * 缓存当前打开轮盘时的 model id，用于重置轮盘数据
     */
    private static String MODEL_ID = "";

    private int x;
    private int y;
    private int selectId = -1;
    private int hoverConfigId = -1;
    private ExtraAnimationButton configButtons = null;
    private Pair<String, Integer> current;

    private final FifoHashMap<String, String> extraAnimationMap;
    private final Map<String, ExtraAnimationButton> buttonMap;
    private final Map<String, FifoHashMap<String, String>> classifyMap;
    private final ModelProperties modelProperties;
    private final CustomPlayerEntity customPlayerEntity;

    public AnimationRouletteScreen(Map<String, ExtraAnimationButton> buttonMap,
                                   Map<String, FifoHashMap<String, String>> classifyMap,
                                   ModelProperties modelProperties,
                                   CustomPlayerEntity customPlayerEntity) {
        super(Component.literal("Animation Roulette GUI"));
        this.modelProperties = modelProperties;
        this.customPlayerEntity = customPlayerEntity;
        this.classifyMap = classifyMap;
        this.buttonMap = buttonMap;

        // 缓存的读取
        this.current = CACHE.peekLast();
        if (this.current != null && this.classifyMap.containsKey(current.getLeft())) {
            this.extraAnimationMap = this.classifyMap.get(current.getLeft());
        } else {
            this.extraAnimationMap = this.modelProperties.extraAnimationOrderMap();
            CACHE.clear();
            CACHE.add(MutablePair.of(StringUtils.EMPTY, this.current == null ? 0 : this.current.getRight()));
            this.current = CACHE.peekLast();
        }
    }

    public AnimationRouletteScreen(String modelId, ModelProperties properties, CustomPlayerEntity customPlayerEntity) {
        super(Component.literal("Animation Roulette GUI"));
        this.modelProperties = properties;
        this.customPlayerEntity = customPlayerEntity;
        this.classifyMap = properties.extraAnimationClassifyMap();
        this.buttonMap = properties.extraAnimationButtonsMap();

        // 如果 ID 和缓存的不一致，重置轮盘缓存
        if (!MODEL_ID.equals(modelId)) {
            CACHE.clear();
            MODEL_ID = modelId;
        }
        // 缓存的读取
        if (CACHE.isEmpty()) {
            CACHE.add(MutablePair.of(StringUtils.EMPTY, 0));
        }
        this.current = CACHE.peekLast();
        if (this.classifyMap.containsKey(current.getLeft())) {
            this.extraAnimationMap = this.classifyMap.get(current.getLeft());
        } else {
            this.extraAnimationMap = properties.extraAnimationOrderMap();
            CACHE.clear();
            CACHE.add(MutablePair.of(StringUtils.EMPTY, this.current.getRight()));
            this.current = CACHE.peekLast();
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();

        this.x = width / 2 - 70;
        this.y = height / 2 - 8;
        if (this.extraAnimationMap.size() < (current.getRight() * MAX_ROULETTE_COUNT + 1)) {
            current.setValue(0);
        }
        if (this.extraAnimationMap.size() <= this.selectId) {
            this.selectId = 0;
        }

        // 翻页按钮
        this.addRenderableWidget(new FlatColorButton(this.x + 125, this.y - 87, 15, 15,
                Component.literal("<"), b -> this.pageUp()));
        this.addRenderableWidget(new FlatColorButton(this.x + 225, this.y - 87, 15, 15,
                Component.literal(">"), b -> this.pageDown()));

        // 添加返回按钮
        Component name = Component.translatable("gui.yes_steve_model.model.return");
        this.addRenderableWidget(new FlatColorButton(this.x + 125, this.y - 70, 115, 15, name, b -> {
            if (CACHE.size() > 1) {
                CACHE.removeLast();
                AnimationRouletteScreen screen = new AnimationRouletteScreen(this.buttonMap, this.classifyMap, this.modelProperties, this.customPlayerEntity);
                this.getMinecraft().setScreen(screen);
            } else {
                this.getMinecraft().setScreen(null);
            }
        }));

        // 配置按钮
        if (configButtons != null) {
            final int[] yOffset = {-53};
            for (ConfigForms configForm : configButtons.getConfigForms()) {
                this.addConfigForms(configForm, yOffset);
            }
        }
    }

    private void addConfigForms(ConfigForms configForm, int[] yOffset) {
        if (configForm instanceof CheckboxForms checkboxForms) {
            this.executeMolang(configForm.value(), result -> {
                FlatCheckbox checkbox = getFlatCheckbox(checkboxForms, result, yOffset);
                this.addRenderableWidget(checkbox);
                yOffset[0] += 14;
            });
        }

        if (configForm instanceof RangeForms rangeForms) {
            this.executeMolang(configForm.value(), result -> {
                FlatSlider slider = getFlatSlider(rangeForms, result, yOffset);
                this.addRenderableWidget(slider);
                yOffset[0] += 17;
            });
        }

        if (configForm instanceof RadioForms radioForms) {
            this.executeMolang(configForm.value(), result -> addRatioButtons(radioForms, result, yOffset));
        }
    }

    private void addRatioButtons(RadioForms radioForms, String result, int[] yOffset) {
        int index = Math.round(transformNumber(result));
        var labels = radioForms.labels();
        if (index < 0 || labels.size() < index) {
            index = 0;
        }

        // 动态改变单选框每行个数
        // 遍历获取最长的行的长度
        int lineMaxWidth = 0;
        for (String labelName : labels.keyList()) {
            lineMaxWidth = Math.max(lineMaxWidth, font.width(labelName) + 16);
        }
        int countPerLine = Math.max(1, 115 / lineMaxWidth);

        Component title = Component.literal(radioForms.title());
        Tooltip description = Tooltip.create(Component.literal(radioForms.description()));
        // 当每行一个时，高度需要 -14
        int maxHeight = (labels.size() / countPerLine + 1) * 14 + (countPerLine > 1 ? 14 : 0);
        FlatRatioBox ratioBox = new FlatRatioBox(this.x + 125, this.y + yOffset[0], maxHeight, title);
        ratioBox.setTooltip(description);
        this.addRenderableOnly(ratioBox);
        yOffset[0] += 14;

        // 遍历添加每个 label
        for (int i = 0; i < labels.size(); i++) {
            Component labelName = Component.literal(labels.getKeyAt(i));
            String labelValue = labels.getValueAt(i);
            boolean isSelected = index == i;

            int perWidth = Math.round(110f / countPerLine);
            int xOffset = this.x + 127 + perWidth * (i % countPerLine);

            FlatCheckbox checkbox = new FlatCheckbox(xOffset, this.y + yOffset[0], perWidth,
                    labelName, data -> {
                executeMolang(labelValue, null);
                this.init();
            });
            checkbox.setStateTriggered(isSelected);

            this.addRenderableWidget(checkbox);

            // 每满 countPerLine 个时，换行
            if (i % countPerLine == (countPerLine - 1)) {
                yOffset[0] += 14;
            }
        }
    }

    @NotNull
    private FlatSlider getFlatSlider(RangeForms rangeForms, String result, int[] yOffset) {
        Component title = Component.literal(rangeForms.title());
        Tooltip description = Tooltip.create(Component.literal(rangeForms.description()));
        int number = Math.round(transformNumber(result));

        FlatSlider slider = new FlatSlider(this.x + 125, this.y + yOffset[0],
                title, number, this.customPlayerEntity, rangeForms.value(), rangeForms.step(), rangeForms.min(), rangeForms.max());
        slider.setTooltip(description);

        return slider;
    }

    @NotNull
    private FlatCheckbox getFlatCheckbox(CheckboxForms checkboxForms, String result, int[] yOffset) {
        Component title = Component.literal(checkboxForms.title());
        Tooltip description = Tooltip.create(Component.literal(checkboxForms.description()));

        float number = transformNumber(result);

        FlatCheckbox checkbox = new FlatCheckbox(this.x + 125, this.y + yOffset[0], title, data -> {
            // 手动拼接 molang 字符串进行赋值操作
            String value = data ? "1" : "0";
            String molang = checkboxForms.value() + "=" + value;
            executeMolang(molang, null);
        });
        checkbox.setStateTriggered(number > 0);
        checkbox.setTooltip(description);

        return checkbox;
    }

    private float transformNumber(String result) {
        float number;
        if ("null".equals(result)) {
            number = 0;
        } else {
            number = Float.parseFloat(result);
        }
        return number;
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        String patText = StringUtils.joinWith(" > ", CACHE.stream().map(Pair::getLeft).toArray());
        graphics.drawCenteredString(font, Component.translatable("gui.yes_steve_model.roulette.path", patText),
                this.x + 180, this.y - 100, 0xFFFFFF);

        this.drawRouletteBg(graphics.pose(), pMouseX, pMouseY);
        this.drawRouletteText(graphics);
        this.drawPageText(graphics);
        super.render(graphics, pMouseX, pMouseY, pPartialTick);
    }

    private void executeMolang(String molang, @Nullable Consumer<String> resultConsumer) {
        try {
            IValue parsed = CustomMolangParser.parseSingleExpressionUnsafe(molang);
            this.customPlayerEntity.executeMolangExp(parsed, resultConsumer);
        } catch (ParseException exception) {
            YesSteveModel.LOGGER.error(exception);
        }
    }

    private void drawPageText(GuiGraphics graphics) {
        graphics.fill(this.x + 142, this.y - 87, this.x + 223, this.y - 72, 0, 0xCF000000);
        String pageText = String.format("%d/%d", current.getRight() + 1, (this.extraAnimationMap.size() - 1) / MAX_ROULETTE_COUNT + 1);
        graphics.drawCenteredString(font, pageText, this.x + 182, this.y - 83, ChatFormatting.AQUA.getColor());
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double scroll) {
        if (scroll < 0) {
            this.pageDown();
            return true;
        }
        if (scroll > 0) {
            this.pageUp();
            return true;
        }
        return false;
    }

    private void pageUp() {
        current.setValue(Math.max(0, current.getRight() - 1));
    }

    private void pageDown() {
        int page = (current.getRight() + 1) * MAX_ROULETTE_COUNT;
        if (this.extraAnimationMap.size() > page) {
            current.setValue(current.getRight() + 1);
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (-1 < selectId && selectId < extraAnimationMap.size()) {
            // 点击普通界面
            this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            String selectKey = extraAnimationMap.getKeyAt(selectId);
            if (selectKey.startsWith(SPEC_PREFIX)) {
                // 如果 key 以 # 开头，那么说明选择的是子页面
                this.clickClassify(selectKey);
            } else {
                // 否则执行默认行为
                this.clickDefault(selectKey);
            }
        } else if (-1 < hoverConfigId && hoverConfigId < extraAnimationMap.size()) {
            // 点击配置按钮
            this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            // 以防万一，再检查一次
            String selectValue = extraAnimationMap.getValueAt(hoverConfigId);
            if (selectValue.startsWith(SPEC_PREFIX)) {
                selectValue = selectValue.substring(SPEC_PREFIX.length());
                if (this.buttonMap.containsKey(selectValue)) {
                    clickConfig(selectValue);
                }
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    private void clickConfig(String buttonName) {
        this.configButtons = this.buttonMap.get(buttonName);
        this.init();
    }

    private void clickDefault(String selectKey) {
        LocalPlayer player = this.getMinecraft().player;
        if (NetworkHandler.isRemoteChannelPresent()) {
            var peekLast = CACHE.peekLast();
            String classifyId = "";
            if (peekLast != null && StringUtils.isNotBlank(peekLast.getLeft())) {
                classifyId = peekLast.getLeft();
            }
            NetworkHandler.CHANNEL.sendToServer(new SetPlayAnimation(selectId, classifyId));
        } else if (player != null) {
            player.getCapability(PlayerGeoCapabilityProvider.CAP)
                    .ifPresent(cap -> cap.playAnimation(selectKey));
        }
        if (player != null && GeneralConfig.PRINT_ANIMATION_ROULETTE_MSG.get()) {
            MutableComponent component = Component.translatable("message.yes_steve_model.model.animation_roulette.play", selectKey);
            player.sendSystemMessage(component);
        }
        this.getMinecraft().setScreen(null);
    }

    private void clickClassify(String selectKey) {
        // 最多让你套 5 层
        if (CACHE.size() > 5) {
            LocalPlayer player = this.getMinecraft().player;
            if (player != null) {
                player.sendSystemMessage(Component.translatable("gui.yes_steve_model.roulette.too_long"));
            }
            return;
        }
        String key = selectKey.substring(SPEC_PREFIX.length());
        FifoHashMap<String, String> map = classifyMap.get(key);
        if (map != null) {
            CACHE.addLast(MutablePair.of(key, 0));
            AnimationRouletteScreen screen = new AnimationRouletteScreen(this.buttonMap, this.classifyMap, this.modelProperties, this.customPlayerEntity);
            this.getMinecraft().setScreen(screen);
        }
    }

    public static void addRootClassify(String keyName) {
        CACHE.clear();
        CACHE.addLast(MutablePair.of("", 0));
        CACHE.addLast(MutablePair.of(keyName, 0));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawRouletteText(GuiGraphics graphics) {
        float startDeg = Mth.PI / MAX_ROULETTE_COUNT;
        int remainSize = extraAnimationMap.size() - current.getRight() * MAX_ROULETTE_COUNT;
        int radius = 65;

        for (int i = 0; i < Math.min(MAX_ROULETTE_COUNT, remainSize); i++) {
            int index = i + current.getRight() * 8;
            int xPos = (int) (x + radius * Mth.cos(startDeg));
            int yPos = (int) (y + radius * Mth.sin(startDeg) - font.lineHeight / 2f);
            String animationValue = extraAnimationMap.getValueAt(index);

            boolean isClassify = extraAnimationMap.getKeyAt(index).startsWith(SPEC_PREFIX);
            boolean hasConfig = animationValue.startsWith(SPEC_PREFIX);

            // 如果含有配置
            if (hasConfig) {
                String configValue = animationValue.substring(SPEC_PREFIX.length());
                if (buttonMap.containsKey(configValue)) {
                    ExtraAnimationButton button = buttonMap.get(configValue);
                    animationValue = button.getName();
                }
            }

            // 绘制文本
            if (StringUtils.isNoneBlank(animationValue)) {
                MutableComponent name = Component.literal(animationValue);
                this.drawAnimationName(graphics, name, xPos, yPos, isClassify);
            } else {
                MutableComponent name = Component.literal(String.valueOf(index));
                graphics.drawCenteredString(font, name, xPos, yPos - 8, 0xF3EFE0);
            }
            // 只有第 0 页显示按键绑定
            // 如果当前是分类菜单，也不显示按键绑定
            if (current.getRight() == 0 && CACHE.size() == 1) {
                this.drawKeyMappingName(graphics, index, xPos, yPos);
            }
            startDeg = startDeg + 2 * Mth.PI / MAX_ROULETTE_COUNT;
        }
    }

    private void drawKeyMappingName(GuiGraphics graphics, int index, int xPos, int yPos) {
        MutableComponent keyText = Component.literal("[ ").withStyle(ChatFormatting.YELLOW);
        KeyMapping keyMapping = ExtraAnimationKey.EXTRA_ANIMATION_KEYS.get(index);
        if (keyMapping.getKey() == InputConstants.UNKNOWN) {
            keyText.append(Component.translatable("key.yes_steve_model.extra_animation.none"));
        } else {
            keyText.append(keyMapping.getTranslatedKeyMessage());
        }
        keyText.append(" ]");
        graphics.drawCenteredString(font, keyText, xPos, yPos + 4, 0xF3EFE0);
    }

    private void drawAnimationName(GuiGraphics graphics, MutableComponent name, int xPos, int yPos, boolean isClassify) {
        int maxLength = 50;
        int lineHeight = font.lineHeight;

        if (isClassify) {
            name = name.withStyle(ChatFormatting.RED);
        }
        var split = font.split(name, maxLength);
        int yoffset = yPos - split.size() * lineHeight + 2;
        // 只有第一页的轮盘能够绑定键位，其他的不行
        // 分类菜单也应该不显示按键
        if (current.getRight() != 0 || CACHE.size() > 1) {
            // 应该下移两行
            yoffset = yoffset + lineHeight;
        }
        for (FormattedCharSequence sequence : split) {
            graphics.drawCenteredString(font, sequence, xPos, yoffset, 0xF3EFE0);
            yoffset = yoffset + lineHeight;
        }
    }

    private void drawRouletteBg(PoseStack pPoseStack, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f pMatrix = pPoseStack.last().pose();

        int count = 8;
        float theta = (float) Mth.atan2(mouseY - y, mouseX - x);
        if (theta < 0) {
            theta = Mth.PI * 2 + theta;
        }
        float distance = Mth.sqrt(Mth.square(mouseY - y) + Mth.square(mouseX - x));
        boolean isSelected = false;
        boolean isConfigSelected = false;

        for (int i = 0; i < Math.min(8, extraAnimationMap.size() - current.getRight() * 8); i++) {
            float spacingDeg = Mth.PI / 90;
            float startDeg = (2 * Mth.PI / count) * i + spacingDeg;
            float endDeg = (2 * Mth.PI / count) * (i + 1) - spacingDeg;
            int index = i + current.getRight() * 8;
            boolean hasConfig = extraAnimationMap.getValueAt(index).startsWith(SPEC_PREFIX);

            isSelected = onDrawFan(startDeg, theta, endDeg, distance, isSelected, hasConfig, i, bufferbuilder, pMatrix);

            boolean isConfigHover = startDeg < theta && theta < endDeg && 20 < distance && distance < 50;
            if (isConfigHover && hasConfig) {
                drawFan(bufferbuilder, pMatrix, 15, 50, startDeg, endDeg, 0xf000ceff);

                isConfigSelected = true;
                this.hoverConfigId = index;
            }
        }
        if (!isSelected) {
            this.selectId = -1;
        }
        if (!isConfigSelected) {
            this.hoverConfigId = -1;
        }

        tesselator.end();
        RenderSystem.disableBlend();
    }

    private boolean onDrawFan(float startDeg, float theta, float endDeg, float distance, boolean isSelected, boolean hasConfig, int i, BufferBuilder bufferbuilder, Matrix4f pMatrix) {
        boolean hovered = startDeg < theta && theta < endDeg && 50 < distance && distance < 100;
        if (hovered) {
            isSelected = true;
            this.selectId = i + current.getRight() * 8;
        }
        if (hovered && i < extraAnimationMap.size()) {
            if (hasConfig) {
                drawFan(bufferbuilder, pMatrix, 50, 115, startDeg, endDeg, 0xf0FFB100);
                drawFan(bufferbuilder, pMatrix, 25, 50, startDeg, endDeg, 0x90000000);
            } else {
                drawFan(bufferbuilder, pMatrix, 25, 115, startDeg, endDeg, 0xf0FFB100);
            }
        } else {
            drawFan(bufferbuilder, pMatrix, 25, 105, startDeg, endDeg, 0x90000000);
        }

        return isSelected;
    }

    private void drawFan(BufferBuilder builder, Matrix4f matrix4f, float rIn, float rOut, float startDeg, float endDeg, int color) {
        float alpha = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        builder.vertex(matrix4f, x + rOut * Mth.cos(startDeg), y + rOut * Mth.sin(startDeg), 0).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix4f, x + rIn * Mth.cos(startDeg), y + rIn * Mth.sin(startDeg), 0).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix4f, x + rIn * Mth.cos(endDeg), y + rIn * Mth.sin(endDeg), 0).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix4f, x + rOut * Mth.cos(endDeg), y + rOut * Mth.sin(endDeg), 0).color(red, green, blue, alpha).endVertex();
    }
}
