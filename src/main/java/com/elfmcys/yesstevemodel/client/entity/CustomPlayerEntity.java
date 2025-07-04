package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.molang.MolangEventWrapper;
import com.elfmcys.yesstevemodel.client.animation.predicate.*;
import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.compat.bettercombat.BetterCombatCompat;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;
import com.elfmcys.yesstevemodel.client.compat.parcool.ParCoolCompat;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.animation.debug.CustomDebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.CodedAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
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

    private final boolean localPlayer;
    protected boolean isPlayingAnimation = false;
    protected String animationName = "idle";
    protected boolean isAnimationDirty = false;

    private final Vector2f headRot = new Vector2f();
    private volatile boolean renderedWithTempChanges = false;

    private boolean fireInitEvent = false;
    private IValue wrappedUpdateHandler = null;
    private List<IValue> syncHandler = null;

    /**
     * 专为 tacz 枪械事件使用的，用来将枪械动画重置
     */
    private boolean tacGunAnimationNeedReload = false;

    public CustomPlayerEntity(AbstractClientPlayer player, boolean localPlayer, boolean asyncUpdate) {
        super(player, asyncUpdate);
        this.localPlayer = localPlayer;
        getDebugInfo().setEnabled(DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE);
        if (player instanceof LocalPlayer) {
            setInitialized();
        }
        registerControllers();
    }

    /**
     * 越往后优先级越高
     */
    @SuppressWarnings("all")
    public void registerControllers() {
        for (int i = 0; i < 8; i++) {
            String controllerName = PRE_PARALLEL_CONTROLLER + i;
            String animationName = String.format("pre_parallel%d", i);
            addAnimationController(new HybridAnimationController(this, controllerName, 0, new ParallelPredicate(animationName)));
        }

        ParCoolCompat.addParcoolPredicate(this);
        addAnimationController(new HybridAnimationController(this, VEHICLE_CONTROLLER, 0.1f, new VehiclePredicate()));

        addAnimationController(new HybridAnimationController(this, PRE_MAIN_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, MAIN_CONTROLLER, 0.1f, new PlayerMainPredicate()));
        addAnimationController(new HybridAnimationController(this, POST_MAIN_CONTROLLER, 0, new EmptyPredicate()));

        addAnimationController(new HybridAnimationController(this, PRE_HOLD_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, HOLD_OFFHAND_CONTROLLER, 0, new OffhandPredicate()));
        addAnimationController(new HybridAnimationController(this, HOLD_MAINHAND_CONTROLLER, 0, new MainhandPredicate()));
        addAnimationController(new HybridAnimationController(this, POST_HOLD_CONTROLLER, 0, new EmptyPredicate()));

        TACZCompat.addTaczPredicate(this);

        addAnimationController(new HybridAnimationController(this, PRE_SWING_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, SWING_CONTROLLER, 0, new SwingPredicate()));
        addAnimationController(new HybridAnimationController(this, POST_SWING_CONTROLLER, 0, new EmptyPredicate()));

        addAnimationController(new HybridAnimationController(this, PRE_USE_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, USE_CONTROLLER, 0.1f, new UsePredicate()));
        addAnimationController(new HybridAnimationController(this, POST_USE_CONTROLLER, 0, new EmptyPredicate()));

        addAnimationController(new HybridAnimationController(this, PASSENGER_CONTROLLER, 0.1f, new PassengerPredicate()));
        CarryOnCompat.addCarryOnPredicate(this);

        // 下面不需要自定义动画控制器
        {
            addAnimationController(new CodedAnimationController(this, CAP_CONTROLLER, 0.1f, new CapPredicate()));
            addAnimationController(new CodedAnimationController(this, HOVER_CONTROLLER, 0, new HoverPredicate()));
            addAnimationController(new CodedAnimationController(this, FOCUS_CONTROLLER, 0, new FocusPredicate()));
        }

        for (int i = 0; i < 8; i++) {
            String controllerName = PARALLEL_CONTROLLER + i;
            String animationName = String.format("parallel%d", i);
            addAnimationController(new HybridAnimationController(this, controllerName, 0,
                    new ParallelPredicate(animationName), true));
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                String controllerName = ARMOR_CONTROLLER + slot.getName();
                addAnimationController(new HybridAnimationController(this, controllerName, 0, new ArmorPredicate(slot)));
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
    public Struct getRoamingStruct() {
        return null;
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

    @Override
    public @Nullable IValue getUserFunction(int name) {
        return ClientModelManager.getUserFunction(modelId, name);
    }

    @Override
    public @Nullable List<IValue> getEventHandler(int name) {
        return ClientModelManager.getMolangEventHandler(modelId, name);
    }

    @Nullable
    @Override
    public GeoAnimationController getAnimationControllerData(String animationControllerName) {
        return ClientModelManager.getModel(getModelId()).map(model -> model.animationControllers().get(animationControllerName)).orElse(null);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation() {
        return ClientModelManager.getPlayerTextureLocation(modelId, textureName).orElse(MissingTextureAtlasSprite.getLocation());
    }

    @Override
    @SuppressWarnings("all")
    public boolean setCustomAnimations(MolangContext ctx, @NotNull AnimationEvent animationEvent) {
        List extraData = animationEvent.getExtraData();
        if (!Minecraft.getInstance().isPaused() && extraData.size() == 1 && extraData.get(0) instanceof EntityModelData
            && entity != null) {
            EntityModelData data = (EntityModelData) extraData.get(0);
            this.recoverLastCodedAnimation();
            boolean update = super.setCustomAnimations(ctx, animationEvent);
            this.codeAnimation(animationEvent, data, update);
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

    @Override
    public boolean updateCurrentModel(boolean force) {
        if (super.updateCurrentModel(force)) {
            var model = getCurrentModel();
            if (model != null && !model.headBones().isEmpty()) {
                var head = model.headBones().get(model.headBones().size() - 1);
                headRot.set(head.getRotationX(), head.getRotationY());
            }
            return true;
        }
        return false;
    }

    private void recoverLastCodedAnimation() {
        var model = getCurrentModel();
        if (model != null && !model.headBones().isEmpty()) {
            var head = model.headBones().get(model.headBones().size() - 1);
            head.setRotationX(headRot.x);
            head.setRotationY(headRot.y);
        }
    }

    @Deprecated
    private void codeAnimation(AnimationEvent<CustomPlayerEntity> animationEvent, EntityModelData data, boolean update) {
        GeoModelState model = getCurrentModel();
        if (model == null) {
            return;
        }

        var head = !model.headBones().isEmpty() ? model.headBones().get(model.headBones().size() - 1) : null;
        // 更新头部旋转
        if (head != null) {
            if (update) {
                headRot.set(head.getRotationX(), head.getRotationY());
            }
            head.setRotationX(headRot.x + (float) Math.toRadians(data.headPitch));
            head.setRotationY(headRot.y + (float) Math.toRadians(data.netHeadYaw));
        }

        // 更新第一人称相机偏移与头部隐藏
        if (animationEvent.getAnimatableEntity().isLocalPlayer()) {
            if (FirstPersonCompat.isInstalled()) {
                if (model.firstPersonHead() != null) {
                    model.firstPersonHead().setHidden(FirstPersonCompat.shouldHideHead());
                }
                if (model.firstPersonViewLocator() != null) {
                    FirstPersonCompat.setHeadPos(model.firstPersonViewLocator().getPivotY() * animationEvent.getAnimatableEntity().getHeightScale());
                } else if (update) {
                    FirstPersonCompat.setHeadPos(head == null ? 24f : (head.getPivotY() * animationEvent.getAnimatableEntity().getHeightScale()));
                }
            }

            if (BetterCombatCompat.isInstalled() && model.firstPersonHead() != null) {
                model.firstPersonHead().setHidden(BetterCombatCompat.shouldHideHead(this));
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
        return ClientModelManager.getModel(modelId)
                .map(model -> model.textures().keyList().indexOf(textureName))
                .filter(i -> i >= 0)
                .orElse(0);
    }

    @Override
    protected void setupModel(GeoModelState model) {
        fireInitEvent = true;
        var updateHandlers = getEventHandler(MolangEventWrapper.PLAYER_UPDATE);
        if (updateHandlers != null) {
            wrappedUpdateHandler = MolangEventWrapper.wrap(updateHandlers);
        } else {
            wrappedUpdateHandler = null;
        }
        syncHandler = getEventHandler(MolangEventWrapper.SYNC);
    }

    @Override
    protected void preAnimationSetup(float seekTime) {
        getAnimationProcessor().putRemoteStruct(getRoamingStruct());
        if (fireInitEvent) {
            fireInitEvent = false;
            var initEvent = getEventHandler(MolangEventWrapper.PLAYER_INIT);
            if (initEvent != null) {
                executeMolangExp(MolangEventWrapper.wrap(initEvent), true, true, null);
            }
        }
        if (wrappedUpdateHandler != null) {
            executeMolangExp(wrappedUpdateHandler, true, true, null);
        }
    }

    public void molangSync(FloatArrayList args) {
        if (syncHandler != null) {
            executeMolangExp(MolangEventWrapper.wrap(syncHandler, args), true, false, null);
        }
    }

    @Override
    public boolean isTacGunAnimationNeedReload() {
        return tacGunAnimationNeedReload;
    }

    @Override
    public void setTacGunAnimationNeedReload(boolean tacGunAnimationNeedReload) {
        this.tacGunAnimationNeedReload = tacGunAnimationNeedReload;
    }
}
