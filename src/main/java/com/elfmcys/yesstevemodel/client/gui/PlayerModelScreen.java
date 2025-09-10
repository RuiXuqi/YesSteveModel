package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.capability.AuthModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.StarModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.AnimationRegister;
import com.elfmcys.yesstevemodel.client.event.DownloadScreenInterModEvent;
import com.elfmcys.yesstevemodel.client.gui.button.*;
import com.elfmcys.yesstevemodel.client.input.PlayerModelScreenKey;
import com.elfmcys.yesstevemodel.client.lang.LanguageManager;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.client.model.ClientModelSyncListener;
import com.elfmcys.yesstevemodel.client.model.ModelPackInfo;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.info.ModelAuthor;
import com.elfmcys.yesstevemodel.info.ModelMetadata;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class PlayerModelScreen extends Screen implements ClientModelSyncListener {
    private static final CustomGuiPlayerEntity[] MODEL_PREVIEW_ENTITY = new CustomGuiPlayerEntity[10];
    private static final String AUTHOR_SEARCH_PREFIX = "@";
    private static final String PACK_SEARCH_PREFIX = "#";

    private static int page = 0;
    private static String pack = "";

    private final HashSet<String> clientNotDisplayModels = Sets.newHashSet();
    private final Map<String, ModelPackInfo> allPacks;

    private Map<String, ClientModel> models = Maps.newHashMap();
    private Map<String, ModelPackInfo> packs = Maps.newHashMap();

    private List<String> modelOrderList;
    private List<String> packOrderList;

    protected int x;
    protected int y;

    private int maxPage;
    private EditBox textField;
    private Category category;

    static {
        for (int i = 0; i < MODEL_PREVIEW_ENTITY.length; i++) {
            CustomGuiPlayerEntity animatedEntity = new CustomGuiPlayerEntity();
            animatedEntity.getPreviewInfo().setPreview(AnimationRegister.IDLE);
            MODEL_PREVIEW_ENTITY[i] = animatedEntity;
        }
    }

    public PlayerModelScreen() {
        super(Component.literal("YSM Player Model GUI"));
        this.category = Category.ALL;
        if (NetworkHandler.isRemoteChannelPresent()) {
            clientNotDisplayModels.addAll(ServerConfig.CLIENT_NOT_DISPLAY_MODELS.get());
        }
        ClientModelManager.addSyncListener(this);
        this.allPacks = new Object2ReferenceOpenHashMap<>(ClientModelManager.getPacks());
    }

    protected ModelButton getModelButton(int xStart, int yStart, boolean needAuth, CustomGuiPlayerEntity animatedEntity, ClientModel model) {
        return new ModelButton(xStart, yStart, needAuth, animatedEntity, model);
    }

    protected PlayerTextureScreen getTextureScreen(PlayerModelScreen parent, String modelId, ClientModel model) {
        return new PlayerTextureScreen(parent, modelId, model);
    }

    protected ModelInfoScreen getModelInfoScreen(PlayerModelScreen parent, ClientModel model) {
        return new ModelInfoScreen(parent, model);
    }

    private Map<String, ClientModel> getPackModels() {
        Map<String, ClientModel> packModels = Maps.newHashMap();
        if (StringUtils.isBlank(pack)) {
            packModels.putAll(ClientModelManager.getModels());
        }
        ClientModelManager.getModels().forEach((k, v) -> {
            if (k.startsWith(pack)) {
                packModels.put(k, v);
            }
            String packPath = ModelIdUtil.splitModelPath(k).right();
            if (StringUtils.isNotBlank(packPath)) {
                String packName = ModelIdUtil.getLastFolderName(packPath);
                allPacks.putIfAbsent(packPath, new ModelPackInfo(packPath, packName, StringUtils.EMPTY, null, null));
            }
        });
        return packModels;
    }

    private Map<String, ModelPackInfo> getPackInfos() {
        Map<String, ModelPackInfo> packInfos = Maps.newHashMap();
        if (StringUtils.isBlank(pack)) {
            return Maps.newHashMap(allPacks);
        }
        allPacks.forEach((k, v) -> {
            if (k.startsWith(pack)) {
                packInfos.put(k, v);
            }
        });
        return packInfos;
    }

    private void calculateModelList() {
        models = Maps.newHashMap();
        packs = Maps.newHashMap();

        if (minecraft == null || minecraft.player == null) {
            return;
        }
        LocalPlayer player = minecraft.player;

        if (this.category == Category.ALL) {
            this.models = this.getPackModels();
            this.packs = this.getPackInfos();
        }

        // 授权部分不显示文件夹
        if (this.category == Category.AUTH) {
            player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(cap -> {
                for (Map.Entry<String, ClientModel> entry : ClientModelManager.getModels().entrySet()) {
                    if (cap.containModel(entry.getKey()) || !entry.getValue().clientModelInfo().isNeedAuth()) {
                        this.models.put(entry.getKey(), entry.getValue());
                    }
                }
            });
        }

        // 收藏部分也不显示文件夹
        if (this.category == Category.STAR) {
            player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).ifPresent(cap -> {
                for (Map.Entry<String, ClientModel> entry : ClientModelManager.getModels().entrySet()) {
                    if (cap.containModel(entry.getKey())) {
                        this.models.put(entry.getKey(), entry.getValue());
                    }
                }
            });
        }

        // 搜索框不区分大小写
        String search;
        if (textField != null) {
            search = this.textField.getValue().toLowerCase(Locale.ENGLISH);
        } else {
            search = StringUtils.EMPTY;
        }

        if (StringUtils.isBlank(search)) {
            // 搜索框为空时，为正常文件树显示模式
            models.entrySet().removeIf(next -> {
                String path = next.getKey();
                Pair<String, String> split = ModelIdUtil.splitModelPath(path);
                // 滤掉黑名单
                if (clientNotDisplayModels.contains(split.left())) {
                    return true;
                }
                // 只保留当前文件夹下的模型
                return !split.right().equals(pack);
            });

            packs.entrySet().removeIf(next -> {
                String path = next.getKey();
                // 只保留当前文件夹下的文件夹
                return !this.shouldKeep(pack, path);
            });
        } else {
            // 搜索框不为空时，为搜索模式，此时不考虑文件树，直接拉平
            models.entrySet().removeIf(next -> {
                String path = next.getKey();
                String id = ModelIdUtil.splitModelPath(path).left();
                return removeModelIf(id, next.getValue(), search);
            });
            packs.entrySet().removeIf(next -> {
                String path = next.getKey();
                String id = ModelIdUtil.splitModelPath(path).left();
                return removePackIf(id, next.getValue(), search);
            });
        }

        // 按照 ID 顺序排序
        this.modelOrderList = Lists.newArrayList(models.keySet());
        this.modelOrderList.sort(String::compareTo);

        this.packOrderList = Lists.newArrayList(packs.keySet());
        this.packOrderList.sort(String::compareTo);

        int maxCount = models.size() + packs.size();
        this.maxPage = (maxCount - 1) / 10;
    }


    // path: 当前目录（如 "" 或 "dir1/dir2/"）
    // candidate: 备选目录（如 "dir1/", "dir1/dir2/dir3/"）
    private boolean shouldKeep(String path, String candidate) {
        if (path.equals(candidate)) {
            return false;
        }
        if (StringUtils.isBlank(path)) {
            // 只保留一级目录
            int first = candidate.indexOf('/');
            return first == candidate.length() - 1 && candidate.lastIndexOf('/') == first;
        } else {
            if (!candidate.startsWith(path)) {
                return false;
            }
            String remain = candidate.substring(path.length());
            int first = remain.indexOf('/');
            return first == remain.length() - 1 && remain.lastIndexOf('/') == first;
        }
    }

    private boolean removePackIf(String id, ModelPackInfo info, String search) {
        // 空搜索字符串不过滤
        if (StringUtils.isBlank(search)) {
            return false;
        }
        // 如果是 # 开头，则仅按文件夹搜索，需要剔除 #
        if (search.startsWith(PACK_SEARCH_PREFIX)) {
            search = search.substring(PACK_SEARCH_PREFIX.length());
        }
        // ID 匹配
        if (id.toLowerCase(Locale.ENGLISH).contains(search)) {
            return false;
        }
        if (info.lang() != null) {
            // 名称匹配
            String name = LanguageManager.getI18n(info, "name", info.name());
            if (name.toLowerCase(Locale.ENGLISH).contains(search)) {
                return false;
            }
            // 描述文本匹配
            String descText = info.desc();
            if (descText == null) {
                return true;
            }
            String desc = LanguageManager.getI18n(info, "description", descText);
            if (desc.toLowerCase(Locale.ENGLISH).contains(search)) {
                return false;
            }
        }
        return true;
    }

    private boolean removeModelIf(String id, ClientModel data, String search) {
        // 滤掉黑名单
        if (clientNotDisplayModels.contains(id)) {
            return true;
        }
        // 空搜索字符串不过滤
        if (StringUtils.isBlank(search)) {
            return false;
        }

        // 如果是 # 开头，则仅按文件夹搜索
        if (search.startsWith(PACK_SEARCH_PREFIX)) {
            return true;
        }

        // 如果是 @ 开头，则仅按作者搜索
        if (search.startsWith(AUTHOR_SEARCH_PREFIX)) {
            String authorSearch = search.substring(AUTHOR_SEARCH_PREFIX.length());
            ModelMetadata metadata = data.modelInfo().metadata();
            if (metadata != null) {
                return noneAuthorMatch(data, authorSearch, metadata);
            }
            return true;
        }

        // ID 不过滤
        if (id.toLowerCase(Locale.ENGLISH).contains(search)) {
            return false;
        }

        ModelMetadata metadata = data.modelInfo().metadata();
        if (metadata != null) {
            // 名称不过滤
            String name = LanguageManager.getI18n(data, "metadata.name", metadata.name()).toLowerCase(Locale.ENGLISH);
            if (name.contains(search)) {
                return false;
            }
            // 描述文本不过滤
            String tips = LanguageManager.getI18n(data, "metadata.tips", metadata.tips()).toLowerCase(Locale.ENGLISH);
            if (tips.contains(search)) {
                return false;
            }
            // 作者名不过滤
            return noneAuthorMatch(data, search, metadata);
        }

        return true;
    }

    public String getParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        // 去掉末尾的斜杠
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int idx = trimmed.lastIndexOf('/');
        if (idx < 0) {
            return "";
        }
        return trimmed.substring(0, idx + 1);
    }

    private boolean noneAuthorMatch(ClientModel data, String search, ModelMetadata metadata) {
        int index = 0;
        for (ModelAuthor author : metadata.authors()) {
            String authorName = LanguageManager.getI18n(data, "metadata.authors.%d.name".formatted(index), author.name()).toLowerCase(Locale.ENGLISH);
            if (authorName.contains(search)) {
                return false;
            }
            index++;
        }
        return true;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.calculateModelList();

        if (page > this.maxPage) {
            page = 0;
        }

        this.x = (width - 420) / 2;
        this.y = (height - 235) / 2;

        String perText = "";
        boolean focus = false;
        if (textField != null) {
            perText = textField.getValue();
            focus = textField.isFocused();
        }
        textField = new EditBox(getMinecraft().font, x + 144, y + 6, 140, 16, Component.literal("YSM Search Box"));
        textField.setValue(perText);
        textField.setTextColor(0xF3EFE0);
        textField.setFocused(focus);
        textField.moveCursorToEnd();
        this.addWidget(this.textField);

        addRenderableWidget(new FlatIconButton(x + 5, y + 5, 20, 20, 80, 16, b -> {
            if (Minecraft.getInstance().player != null) {
                LocalPlayer player = Minecraft.getInstance().player;
                player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                    var model = cap.getModelContainer();
                    if (model.modelInfo().metadata() != null) {
                        Minecraft.getInstance().setScreen(getModelInfoScreen(this, model));
                    }
                });
            }
        })).setTooltips("gui.yes_steve_model.model.info");
        addRenderableWidget(new FlatIconButton(x + 28, y + 5, 79, 20, 32, 16, (b) -> {
            if (Minecraft.getInstance().player != null) {
                LocalPlayer player = Minecraft.getInstance().player;
                player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                    var model = cap.getModelContainer();
                    Minecraft.getInstance().setScreen(getTextureScreen(this, cap.getModelId(), model));
                });
            }
        }).setTooltips("gui.yes_steve_model.model.texture"));
        addRenderableWidget(new StarButton(x + 110, y + 5));

        // 添加返回按钮
        if (StringUtils.isNotBlank(pack)) {
            addRenderableWidget(new FlatIconButton(x + 110, y + 27, 20, 20, 0, 32, (b) -> {
                pack = this.getParentPath(pack);
                page = 0;
                this.init();
            }).setTooltips("gui.back"));
        }

        addRenderableWidget(new FlatIconButton(x + 328, y + 5, 18, 18, 32, 0, (b) -> {
            if (this.category != Category.ALL) {
                this.category = Category.ALL;
                page = 0;
                this.init();
            }
        }).setTooltips("gui.yes_steve_model.all_models"));
        addRenderableWidget(new FlatIconButton(x + 308, y + 5, 18, 18, 48, 0, (b) -> {
            if (this.category != Category.AUTH) {
                this.category = Category.AUTH;
                page = 0;
                this.init();
            }
        }).setTooltips("gui.yes_steve_model.auth_models"));
        addRenderableWidget(new FlatIconButton(x + 288, y + 5, 18, 18, 0, 0, (b) -> {
            if (this.category != Category.STAR) {
                this.category = Category.STAR;
                page = 0;
                this.init();
            }
        }).setTooltips("gui.yes_steve_model.star_models"));

        addRenderableWidget(new FlatIconButton(x + 397, y + 5, 18, 18, 16, 16, (b) -> {
            this.getMinecraft().setScreen(new ConfigScreen(this));
        }).setTooltips("gui.yes_steve_model.config"));
        addRenderableWidget(new FlatIconButton(x + 377, y + 5, 18, 18, 0, 16, (b) -> {
            DownloadScreenInterModEvent.openDownloadScreen(this);
        }).setTooltips("gui.yes_steve_model.download"));
        addRenderableWidget(new FlatIconButton(x + 357, y + 5, 18, 18, 80, 0, (b) -> {
            this.getMinecraft().setScreen(new OpenModelFolderScreen(this));
        }).setTooltips("gui.yes_steve_model.open_model_folder.open"));

        addRenderableWidget(new FlatColorButton(x + 198, y + 215, 52, 14, Component.translatable("gui.yes_steve_model.pre_page"), (b) -> {
            if (page > 0) {
                page--;
                this.init();
            }
        }));
        addRenderableWidget(new FlatColorButton(x + 308, y + 215, 52, 14, Component.translatable("gui.yes_steve_model.next_page"), (b) -> {
            if (page < this.maxPage) {
                page++;
                this.init();
            }
        }));

        // 先是文件夹
        int startIndex = 0;
        for (int i = 0; i < 10; i++) {
            int packIndex = i + page * 10;
            if (packIndex >= packOrderList.size()) {
                break;
            }
            String id = packOrderList.get(packIndex);
            int xStart = x + 143 + 55 * (i % 5);
            int yStart = y + 28 + 93 * (i / 5);
            this.getPack(id).ifPresent(packInfo -> {
                this.addRenderableWidget(new PackButton(xStart, yStart, 52, 90, packInfo, b -> {
                    pack = id;
                    page = 0;
                    this.init();
                }));
            });
            startIndex++;
        }

        // 满了，就不加了
        if (startIndex >= 10) {
            return;
        }

        if (minecraft == null || minecraft.player == null) {
            return;
        }
        LazyOptional<AuthModelsCapability> authModels = minecraft.player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP);
        for (int i = 0; i < 10 - startIndex; i++) {
            int modelIndex = i + page * 10;
            if (modelIndex >= models.size()) {
                break;
            }
            String id = modelOrderList.get(modelIndex);
            int posIndex = i + startIndex;
            int xStart = x + 143 + 55 * (posIndex % 5);
            int yStart = y + 28 + 93 * (posIndex / 5);
            final CustomGuiPlayerEntity animatedEntity = MODEL_PREVIEW_ENTITY[i];
            authModels.ifPresent(cap -> {
                var model = models.get(id);
                boolean needAuth = model.clientModelInfo().isNeedAuth() && !cap.getAuthModels().contains(id);

                animatedEntity.updateModelAndTexture(id, model.playerModel().defaultTextureName());
                animatedEntity.getPreviewInfo().setPreview(model.modelInfo().properties().previewAnimation());
                addRenderableWidget(getModelButton(xStart, yStart, needAuth, animatedEntity, model));
            });
        }
    }

    @Override
    @SuppressWarnings("all")
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float frameDeltaTime) {
        renderBackground(graphics);

        graphics.fillGradient(x, y, x + 135, y + 235, 0xff_222222, 0xff_222222);
        graphics.fillGradient(x + 138, y, x + 420, y + 235, 0xff_222222, 0xff_222222);
        graphics.fillGradient(x + 351, y + 7, x + 352, y + 21, 0xFF_F3EFE0, 0xFF_F3EFE0);

        textField.render(graphics, mouseX, mouseY, frameDeltaTime);
        renderReferenceEntity(graphics, mouseX, mouseY, minecraft.getFrameTime());

        if (textField.getValue().isEmpty() && !textField.isFocused()) {
            graphics.drawString(font, Component.translatable("gui.yes_steve_model.search").withStyle(ChatFormatting.ITALIC), x + 148, y + 10, 0x777777);
        }

        String pageInfo = String.format("%d/%d", page + 1, this.maxPage + 1);
        graphics.drawString(font, pageInfo, x + 138 + (282 - font.width(pageInfo)) / 2, y + 223 - font.lineHeight / 2, 0xF3EFE0);

        String debugInfo = String.format("%s-%s", SharedConstants.getCurrentVersion().getName(), ModList.get().getModFileById(YesSteveModel.MOD_ID).versionString());
        graphics.drawString(font, debugInfo, x + 2, y + 226, ChatFormatting.DARK_GRAY.getColor());

        if (StringUtils.isNotBlank(pack)) {
            MutableComponent path = Component.literal("\uD83D\uDCC2 " + pack).withStyle(ChatFormatting.GRAY);
            int i = 0;
            List<FormattedCharSequence> split = font.split(path, 270);
            for (FormattedCharSequence sequence : split) {
                int offset = -(split.size() - i) * 10 - 2;
                graphics.drawString(font, sequence, x + 142, y + offset, 0xF3EFE0);
                i++;
            }
        }
        drawSyncState(graphics);

        super.render(graphics, mouseX, mouseY, frameDeltaTime);
        this.renderables.stream().filter(r -> r instanceof FlatIconButton)
                .forEach(r -> ((FlatIconButton) r).renderToolTip(graphics, this, mouseX, mouseY));
        this.renderables.stream().filter(r -> r instanceof ModelButton)
                .forEach(r -> ((ModelButton) r).renderComponentTooltip(graphics, this, mouseX, mouseY));
        this.renderables.stream().filter(r -> r instanceof PackButton)
                .forEach(r -> ((PackButton) r).renderComponentTooltip(graphics, this, mouseX, mouseY));
    }

    @SuppressWarnings("DataFlowIssue")
    private void drawSyncState(GuiGraphics graphics) {
        var state = ClientModelManager.getSyncState();

        Component text;
        switch (state.getType()) {
            case WAITING: {
                text = Component.translatable("gui.yes_steve_model.sync_hint.waiting");
            }
            break;
            case LOADING: {
                text = Component.translatable("gui.yes_steve_model.sync_hint.loading");
            }
            break;
            case PREPARING: {
                text = Component.translatable("gui.yes_steve_model.sync_hint.preparing");
            }
            break;
            case SYNCING: {
                if (state.getReceived() == 0) {
                    text = Component.translatable("gui.yes_steve_model.sync_hint.syncing");
                } else {
                    text = Component.literal(String.format("%s/%s", state.getReceived(), state.getTotal()));
                }
            }
            break;
            default:
                return;
        }

        var x = this.x + 414 - font.width(text);
        var y = this.y + 215 + Math.round((14 - font.lineHeight) / 2f);

        graphics.drawString(font, text, x, y, ChatFormatting.DARK_GRAY.getColor());
    }

    protected void renderReferenceEntity(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            Window window = Minecraft.getInstance().getWindow();
            double scale = window.getGuiScale();
            int scissorX = (int) ((this.x + 5) * scale);
            int scissorY = (int) (window.getHeight() - ((this.y + 200) * scale));
            int scissorW = (int) (125 * scale);
            int scissorH = (int) (171 * scale);
            RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 67, y + 190, 70, x + 67 - mouseX, y + 180 - 95 - mouseY, player);
            RenderSystem.disableScissor();

            player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                String[] modelName = {""};
                ClientModelManager.getModel(cap.getModelId()).ifPresent(model -> {
                    ModelMetadata metadata = model.modelInfo().metadata();
                    if (metadata != null) {
                        modelName[0] = LanguageManager.getI18n(model, "metadata.name", metadata.name());
                    }
                });
                if (StringUtils.isBlank(modelName[0])) {
                    modelName[0] = cap.getModelId();
                }
                List<FormattedCharSequence> modelNameSplit = font.split(FormattedText.of(modelName[0]), 125);
                int lineY = y + 205;
                for (FormattedCharSequence line : modelNameSplit) {
                    int nameWidth = font.width(line);
                    graphics.drawString(font, line, x + (135 - nameWidth) / 2, lineY, 0xF3EFE0);
                    lineY += 10;
                }
            });
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String value = this.textField.getValue();
        super.resize(minecraft, width, height);
        this.textField.setValue(value);
    }

    @Override
    public void tick() {
        this.textField.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.textField.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.textField);
            return true;
        } else if (this.textField.isFocused()) {
            this.textField.setFocused(false);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (textField == null) {
            return false;
        }
        String perText = this.textField.getValue();
        if (this.textField.charTyped(codePoint, modifiers)) {
            if (!Objects.equals(perText, this.textField.getValue())) {
                page = 0;
                this.init();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (shouldCloseKey(keyCode, scanCode, modifiers)) {
            return true;
        }
        boolean hasKeyCode = InputConstants.getKey(keyCode, scanCode).getNumericKeyValue().isPresent();
        String preText = this.textField.getValue();
        if (hasKeyCode) {
            return true;
        }
        if (this.textField.keyPressed(keyCode, scanCode, modifiers)) {
            if (!Objects.equals(preText, this.textField.getValue())) {
                page = 0;
                this.init();
            }
            return true;
        } else {
            return this.textField.isFocused() && this.textField.isVisible() && keyCode != 256 || super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    private boolean shouldCloseKey(int keyCode, int scanCode, int modifiers) {
        if (PlayerModelScreenKey.PLAYER_MODEL_KEY.matches(keyCode, scanCode) && !this.textField.isFocused()) {
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    protected void insertText(String text, boolean overwrite) {
        if (overwrite) {
            this.textField.setValue(text);
        } else {
            this.textField.insertText(text);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (minecraft == null) {
            return false;
        }
        if (delta != 0 && inRange(mouseX, mouseY)) {
            return scrollPage(delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean inRange(double mouseX, double mouseY) {
        boolean isInWidthRange = (x + 143) < mouseX && mouseX < (x + 430);
        boolean isInHeightRange = (y + 25) < mouseY && mouseY < (y + 235);
        return isInWidthRange && isInHeightRange;
    }

    private boolean scrollPage(double delta) {
        if (delta > 0 && page > 0) {
            page--;
            getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.init();
        }
        if (delta < 0 && page < this.maxPage) {
            page++;
            getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.init();
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onAlterModels(Map<String, ClientModel> models) {
        init();
    }

    @Override
    public void onNewModelLoaded(Map<String, ClientModel> models, String newModelId, ClientModel newModel) {
        init();
    }

    private Optional<ModelPackInfo> getPack(String id) {
        return Optional.ofNullable(this.allPacks.get(id));
    }

    private enum Category {
        /**
         * 不同页面类别
         */
        ALL, AUTH, STAR
    }
}
