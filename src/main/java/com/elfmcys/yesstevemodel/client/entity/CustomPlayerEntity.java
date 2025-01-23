package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.AnimationManager;
import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.instance.CustomDebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.CodedAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationMolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.roaming.RoamingStruct;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.molang.runtime.HashMapStruct;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.*;

public class CustomPlayerEntity extends AnimatableEntity<AbstractClientPlayer> {
    private String modelId = ModelIdUtil.DEFAULT_MODEL_ID;
    private String textureName = ModelIdUtil.DEFAULT_TEXTURE_NAME;

    private String previewAnimation = "";
    private String hoverAnimation = "";
    private String focusAnimation = "";

    private Struct remoteStruct;
    private int roamingStructInstanceIdOverride;
    private Object2FloatOpenHashMap<String> initialVariables;

    private final boolean localPlayer;
    protected boolean isPlayingAnimation = false;
    protected String animationName = "idle";
    protected boolean isAnimationDirty = false;

    private final Vector2f headRot = new Vector2f();
    private volatile boolean renderedWithTempChanges = false;

    /**
     * 专为 tacz 枪械事件使用的，用来将枪械动画重置
     */
    public boolean tacGunAnimationNeedReload = false;

    public CustomPlayerEntity(AbstractClientPlayer player, boolean localPlayer, boolean asyncUpdate) {
        super(player, asyncUpdate);
        this.localPlayer = localPlayer;
        getDebugInfo().setEnabled(DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE);
        if (player instanceof LocalPlayer) {
            setInitialized();
        }
        if (localPlayer) {
            remoteStruct = new RoamingStruct();
        } else {
            remoteStruct = new HashMapStruct();
        }
        registerControllers();
    }

    /**
     * 越往后优先级越高
     */
    @SuppressWarnings("all")
    public void registerControllers() {
        AnimationManager manager = AnimationManager.getInstance();
        for (int i = 0; i < 8; i++) {
            String controllerName = String.format("pre_parallel_%d_controller", i);
            String animationName = String.format("pre_parallel%d", i);
            addAnimationController(new HybridAnimationController(this, controllerName, 0,
                    (event, evaluator) -> manager.predicateParallel(event, animationName)));
        }

        addAnimationController(new HybridAnimationController(this, MAIN_CONTROLLER, 2,
                (event, evaluator) -> manager.predicateMain(event)));

        addAnimationController(new HybridAnimationController(this, HOLD_OFFHAND_CONTROLLER, 0,
                (event, evaluator) -> manager.predicateOffhandHold(event)));

        addAnimationController(new HybridAnimationController(this, HOLD_MAINHAND_CONTROLLER, 0,
                (event, evaluator) -> manager.predicateMainhandHold(event)));

        addAnimationController(new HybridAnimationController(this, FIRE_MAINHAND_CONTROLLER, 0,
                (event, evaluator) -> manager.predicateMainhandFire(event)));

        addAnimationController(new HybridAnimationController(this, SWING_CONTROLLER, 0,
                (event, evaluator) -> manager.predicateSwing(event)));

        addAnimationController(new HybridAnimationController(this, USE_CONTROLLER, 2,
                (event, evaluator) -> manager.predicateUse(event)));

        addAnimationController(new HybridAnimationController(this, PASSENGER_CONTROLLER, 2,
                (event, evaluator) -> manager.predicatePassengerAnimation(event)));

        if (CarryOnCompat.isInstalled()) {
            addAnimationController(new HybridAnimationController(this, CARRY_ON_CONTROLLER, 2,
                    (event, evaluator) -> CarryOnCompat.predicateCarryOn(event)));
        }

        // 下面不需要自定义动画控制器
        {
            addAnimationController(new CodedAnimationController(this, CAP_CONTROLLER, 2,
                    (event, evaluator) -> manager.predicateCap(event)));

            addAnimationController(new CodedAnimationController(this, HOVER_CONTROLLER, 0,
                    (event, evaluator) -> manager.predicateHover(event)));

            addAnimationController(new CodedAnimationController(this, FOCUS_CONTROLLER, 0,
                    (event, evaluator) -> manager.predicateFocus(event)));
        }

        for (int i = 0; i < 8; i++) {
            String controllerName = String.format("parallel_%d_controller", i);
            String animationName = String.format("parallel%d", i);
            addAnimationController(new HybridAnimationController(this, controllerName, 0,
                    (event, evaluator) -> manager.predicateParallel(event, animationName), true));
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                String controllerName = String.format("%s_controller", slot.getName());
                addAnimationController(new HybridAnimationController(this, controllerName, 0,
                        (event, evaluator) -> manager.predicateArmor(event, slot)));
            }
        }
    }

    @Override
    public String getModelId() {
        if (isModelPresent()) {
            return modelId;
        }
        return ModelIdUtil.DEFAULT_MODEL_ID;
    }

    @Override
    public float getHeightScale() {
        return ClientModelManager.getModel(modelId).map(model -> model.modelInfo().properties().heightScale()).orElse(0.7f);
    }

    @Override
    public float getWidthScale() {
        return ClientModelManager.getModel(modelId).map(model -> model.modelInfo().properties().widthScale()).orElse(0.7f);
    }

    public String getTextureName() {
        if (isModelPresent()) {
            return textureName;
        }
        return ModelIdUtil.DEFAULT_TEXTURE_NAME;
    }

    public void setTextureName(String textureName) {
        this.textureName = textureName;
    }

    public String getPreviewAnimation() {
        return previewAnimation;
    }

    public void setPreviewAnimation(String previewAnimation) {
        this.previewAnimation = previewAnimation;
    }

    public boolean hasPreviewAnimation() {
        return StringUtils.isNoneBlank(this.previewAnimation);
    }

    public boolean hasPreviewAnimation(String previewAnimation) {
        return hasPreviewAnimation() && previewAnimation.equals(this.previewAnimation);
    }

    public String getHoverAnimation() {
        return hoverAnimation;
    }

    public String getFocusAnimation() {
        return focusAnimation;
    }

    public void setHoverAnimation(String hoverAnimation) {
        this.hoverAnimation = hoverAnimation;
    }

    public void setFocusAnimation(String focusAnimation) {
        this.focusAnimation = focusAnimation;
    }

    @Nullable
    public Struct getRemoteStruct() {
        if (initialVariables != null) {
            if (remoteStruct instanceof RoamingStruct roamingStruct) {
                roamingStruct.reset(roamingStructInstanceIdOverride, initialVariables);
            } else {
                remoteStruct = new HashMapStruct();
                for (var entry : initialVariables.object2FloatEntrySet()) {
                    this.remoteStruct.putProperty(StringPool.computeIfAbsent(entry.getKey()), entry.getFloatValue());
                }
            }
            initialVariables = null;
        }
        return remoteStruct;
    }

    public void setRemoteVariables(int instanceId, Object2FloatOpenHashMap<String> initialVariables) {
        this.roamingStructInstanceIdOverride = instanceId;
        this.initialVariables = initialVariables;
    }

    public boolean isLocalPlayer() {
        return localPlayer;
    }

    @Override
    public GeoModel getModel() {
        return ClientModelManager.getModel(modelId).map(ClientModel::mainModel).orElse(ClientModelManager.getDefaultModel().mainModel());
    }

    @Nullable
    @Override
    public Animation getAnimation(String name) {
        return ClientModelManager.getPlayerAnimation(modelId, name)
                .orElse(null);
    }

    @Nullable
    @Override
    public GeoAnimationController getAnimationControllerData(String animationControllerName) {
        return ClientModelManager.getModel(getModelId()).map(model -> model.animationControllers().get(animationControllerName)).orElse(null);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation() {
        if (isModelPresent()) {
            return ClientModelManager.getPlayerTextureLocation(modelId, textureName).orElse(MissingTextureAtlasSprite.getLocation());
        } else {
            return ModelIdUtil.DEFAULT_TEXTURE_LOCATION;
        }
    }

    @Override
    @SuppressWarnings("all")
    public boolean setCustomAnimations(AnimationMolangContext ctx, @NotNull AnimationEvent animationEvent) {
        List extraData = animationEvent.getExtraData();
        if (!Minecraft.getInstance().isPaused() && extraData.size() == 1 && extraData.get(0) instanceof EntityModelData
            && entity != null) {
            EntityModelData data = (EntityModelData) extraData.get(0);
            boolean update = super.setCustomAnimations(ctx, animationEvent);
            this.codeAnimation(animationEvent, data, entity, update);
            return update;
        } else {
            return super.setCustomAnimations(ctx, animationEvent);
        }
    }

    /**
     * 注意非幂等
     */
    @Override
    public boolean shouldForceUpdate() {
        if (FirstPersonCompat.isRenderingPlayer() || RenderUtil.isRenderingEntitiesInInventory()) {
            renderedWithTempChanges = true;
            return true;
        }
        if (renderedWithTempChanges) {
            renderedWithTempChanges = false;
            return true;
        }
        return false;
    }

    @Deprecated
    private void codeAnimation(AnimationEvent<CustomPlayerEntity> animationEvent, EntityModelData data, Player player, boolean update) {
        // 2023/6/21 这一块设计应该改成 molang 的，而且这个寻找效率低下
        // 2023/11/07 改善了寻找效率
        IBone head = getBone("Head");
        GeoModelState model = getCurrentModel();

        // 更新头部旋转
        if (head != null) {
            if (update) {
                headRot.set(head.getRotationX(), head.getRotationY());
            }
            head.setRotationX(headRot.x + (float) Math.toRadians(data.headPitch));
            head.setRotationY(headRot.y + (float) Math.toRadians(data.netHeadYaw));
        }

        // 更新第一人称相机偏移与头部隐藏
        if (animationEvent.getAnimatableEntity().isLocalPlayer() && FirstPersonCompat.isInstalled()) {
            if (model.firstPersonHead() != null) {
                model.firstPersonHead().setHidden(FirstPersonCompat.shouldHideHead());
            }

            if (model != null && model.firstPersonViewLocator() != null) {
                FirstPersonCompat.setHeadPos(model.firstPersonViewLocator().getPivotY() * animationEvent.getAnimatableEntity().getHeightScale());
            } else if (update) {
                FirstPersonCompat.setHeadPos(head == null ? 24f : (head.getPivotY() * animationEvent.getAnimatableEntity().getHeightScale()));
            }
        }
    }

    @Override
    public DebugSource getDebugSource() {
        if (DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE) {
            return CustomDebugSource.INSTANCE;
        } else {
            return null;
        }
    }

    @Override
    public boolean isModelPresent() {
        return ClientModelManager.getModels().containsKey(modelId);
    }

    public void setModelAndTexture(String modelId, String textureName) {
        setInitialized();
        this.modelId = modelId;
        this.textureName = textureName;
    }

    public void playAnimation(String animationName) {
        if (ClientModelManager.getPlayerAnimation(getModelId(), animationName).isPresent()) {
            this.animationName = animationName;
            this.isPlayingAnimation = true;
            this.isAnimationDirty = true;
        } else {
            this.isPlayingAnimation = false;
        }
    }

    public boolean isAnimationDirty() {
        return isAnimationDirty;
    }

    public void clearAnimationDirty() {
        this.isAnimationDirty = false;
    }

    public boolean isPlayingAnimation() {
        return isPlayingAnimation;
    }

    public String getAnimationName() {
        return this.animationName;
    }

    public void stopAnimation() {
        this.isPlayingAnimation = false;
    }

    @Override
    public int getTextureIndex() {
        if (isModelPresent()) {
            return ClientModelManager.getModel(modelId).map(model -> model.textures().keyList().indexOf(textureName)).filter(i -> i >= 0).orElse(0);
        } else {
            return 0;
        }
    }

    @Override
    protected void preAnimationSetup(double seekTime) {
        getAnimationProcessor().putRemoteStruct(getRemoteStruct());
    }
}
