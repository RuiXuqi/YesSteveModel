package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.StarModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.client.animation.AnimationRegister;
import com.elfmcys.yesstevemodel.client.event.RegisterEntityRenderersEvent;
import com.elfmcys.yesstevemodel.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.yesstevemodel.client.lang.LanguageManager;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.info.ModelMetadata;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SetModelAndTexture;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("removal")
public class ModelButton extends Button {
    private final static ResourceLocation ICON = new ResourceLocation(YesSteveModel.MOD_ID, "texture/icon.png");
    protected final boolean needAuth;
    private final int color;
    protected final ClientModel model;
    protected final CustomGuiPlayerEntity animatedEntity;
    private final String hoverAnimationName;
    private final String hoverFadeoutAnimationName;
    private final String focusAnimationName;
    private final double fadeoutTime;
    private final boolean disablePreviewRotation;

    private @Nullable ResourceLocation background = null;
    private @Nullable ResourceLocation foreground = null;

    private @Nullable String locale;
    private @Nullable List<Component> displayInfo = null;
    private @Nullable List<Component> detailedDisplayInfo = null;

    private long hoverTime = -1L;

    public ModelButton(int pX, int pY, boolean needAuth, CustomGuiPlayerEntity animatedEntity, ClientModel model) {
        super(pX, pY, 52, 90, getModelName(animatedEntity, model), (b) -> {
        }, DEFAULT_NARRATION);
        this.needAuth = needAuth;
        this.color = needAuth ? 0x7F_000000 : 0xFF_434242;
        this.model = model;
        this.animatedEntity = animatedEntity;
        this.disablePreviewRotation = model.info().properties().disablePreviewRotation();

        // 获取模型信息中的 GUI 图片
        this.background = model.clientInfo().guiBackground();
        this.foreground = model.clientInfo().guiForeground();

        var animations = model.playerModel().animations();
        // 如果有 hover 动画
        if (animations.containsKey(AnimationRegister.HOVER)) {
            this.hoverAnimationName = AnimationRegister.HOVER;
        } else {
            this.hoverAnimationName = AnimationRegister.EMPTY;
        }

        if (animations.containsKey(AnimationRegister.HOVER_FADEOUT)) {
            this.hoverFadeoutAnimationName = AnimationRegister.HOVER_FADEOUT;
            this.fadeoutTime = animations.get(AnimationRegister.HOVER_FADEOUT).animationLength * 50;
        } else {
            this.hoverFadeoutAnimationName = AnimationRegister.EMPTY;
            this.fadeoutTime = 0;
        }

        // 如果有 focus 动画
        if (animations.containsKey(AnimationRegister.FOCUS)) {
            this.focusAnimationName = AnimationRegister.FOCUS;
        } else {
            this.focusAnimationName = AnimationRegister.EMPTY;
        }
    }

    private static MutableComponent getModelName(CustomGuiPlayerEntity animatedEntity, ClientModel model) {
        ModelMetadata metadata = model.info().metadata();
        if (metadata == null || StringUtils.isBlank(metadata.name())) {
            if (animatedEntity.getModelId().endsWith(".ysm") || animatedEntity.getModelId().endsWith(".zip")) {
                return Component.literal(animatedEntity.getModelId().substring(0, animatedEntity.getModelId().length() - 4));
            } else {
                return Component.literal(animatedEntity.getModelId());
            }
        }
        return Component.literal(LanguageManager.getI18n(model, "metadata.name", metadata.name()));
    }

    @Override
    public void onPress() {
        if (needAuth) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                if (NetworkHandler.isRemoteChannelPresent()) {
                    if (cap.hasRoamingStorage(animatedEntity.getModelContainer().info().hashShort())) {
                        cap.updateModelAndTexture(animatedEntity.getModelId(), animatedEntity.getTextureName());
                        NetworkHandler.sendToServer(new SetModelAndTexture(cap.getModelId(), cap.getTextureName()));
                    } else {
                        NetworkHandler.sendToServer(new SetModelAndTexture(animatedEntity.getModelId(), animatedEntity.getTextureName()));
                    }
                } else {
                    cap.updateModelAndTexture(animatedEntity.getModelId(), animatedEntity.getTextureName());
                }
            });
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float frameDeltaTime) {
        // 计算悬停、fade 动画
        var animInfo = animatedEntity.getPreviewInfo();
        if (isHovered()) {
            hoverTime = Util.getMillis();
            animInfo.setHover(hoverAnimationName);
        } else {
            if (Util.getMillis() - hoverTime < fadeoutTime) {
                animInfo.setHover(this.hoverFadeoutAnimationName);
            } else {
                animInfo.setHover(AnimationRegister.EMPTY);
            }
        }
        if (isFocused()) {
            animInfo.setFocus(focusAnimationName);
        } else {
            animInfo.setFocus(AnimationRegister.EMPTY);
        }

        // 渲染背景
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int x = this.getX();
        int y = this.getY();

        // 灰色底色背景
        graphics.fillGradient(x, y, x + this.width, y + this.height, this.color, this.color);

        // 如果有背景图片，渲染背景图片
        if (this.background != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(this.background, x, y, 0, 0, this.width, this.height - 20, this.width, this.height - 20);
            RenderSystem.disableBlend();
        }

        // 渲染模型
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int scissorX = (int) (x * scale);
        int scissorY = (int) (window.getHeight() - ((y + this.height - 20) * scale));
        int scissorW = (int) (this.width * scale);
        int scissorH = (int) ((this.height - 20) * scale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
        RenderUtil.renderModelInGui(x + this.width / 2f, y + this.height / 2f + 20f, 30f, minecraft.getFrameTime(), animatedEntity, RegisterEntityRenderersEvent.getPlayerRenderer(), disablePreviewRotation, true);
        RenderSystem.disableScissor();

        int z = 3500;
        // 如果有前景图片，渲染前景图片
        if (this.foreground != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(this.foreground, x, y, z, 0, 0, this.width, this.height - 20, this.width, this.height - 20);
            RenderSystem.disableBlend();
        }

        // 渲染模型文本
        Component message = this.getMessage();
        List<FormattedCharSequence> split = font.split(message, 45);
        if (split.size() > 1) {
            graphics.drawCenteredString(font, split.get(0), x + this.width / 2, y + this.height - 19, 0xF3EFE0);
            graphics.drawCenteredString(font, split.get(1), x + this.width / 2, y + this.height - 10, 0xF3EFE0);
        } else {
            graphics.drawCenteredString(font, this.getMessage(), x + this.width / 2, y + this.height - 15, 0xF3EFE0);
        }

        // 渲染悬停效果
        if (!this.needAuth && this.isHoveredOrFocused()) {
            graphics.fillGradient(x, y + 1, x + 1, y + this.height - 1, z, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(x, y, x + this.width, y + 1, z, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(x + this.width - 1, y + 1, x + this.width, y + this.height - 1, z, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(x, y + this.height - 1, x + this.width, y + this.height, z, 0xff_F3EFE0, 0xff_F3EFE0);
        }

        // 未授权模型，渲染黑色遮罩
        if (needAuth) {
            graphics.fillGradient(x, y, x + this.width, y + this.height, z, 0x9f_222222, 0x9f_222222);
        }

        // 如果是 start 模型，渲染星标
        if (minecraft.player != null) {
            minecraft.player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).ifPresent(cap -> {
                if (cap.containModel(animatedEntity.getModelId())) {
                    graphics.blit(ICON, x + this.width - 14, y, z, 16, 0, 16, 16, 256, 256);
                }
            });
        }
    }

    public void renderComponentTooltip(GuiGraphics graphics, Screen screen, int pMouseX, int pMouseY) {
        if (this.isHovered()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0f, 0f, 4000);
            var currentLocale = Minecraft.getInstance().getLanguageManager().getSelected();
            if (!Objects.equals(this.locale, currentLocale)) {
                this.locale = currentLocale;
                this.detailedDisplayInfo = null;
                this.displayInfo = null;
            }
            if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_LSHIFT) ||
                    InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_RSHIFT)) {
                if (this.detailedDisplayInfo == null) {
                    this.detailedDisplayInfo = LanguageManager.buildDisplayInfo(model, currentLocale, animatedEntity.getModelId(), true);
                }
                graphics.renderComponentTooltip(screen.getMinecraft().font, this.detailedDisplayInfo, pMouseX, pMouseY);
            } else {
                if (this.displayInfo == null) {
                    this.displayInfo = LanguageManager.buildDisplayInfo(model, currentLocale, animatedEntity.getModelId(), false);
                }
                graphics.renderComponentTooltip(screen.getMinecraft().font, this.displayInfo, pMouseX, pMouseY);
            }
            graphics.pose().popPose();
        }
    }


    @Override
    protected boolean clicked(double pMouseX, double pMouseY) {
        return !this.needAuth && super.clicked(pMouseX, pMouseY);
    }
}
