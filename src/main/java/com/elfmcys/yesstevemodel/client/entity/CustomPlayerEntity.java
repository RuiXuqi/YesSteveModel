package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.molang.MolangEventWrapper;
import com.elfmcys.yesstevemodel.client.animation.molang.PhysicsManager;
import com.elfmcys.yesstevemodel.client.animation.predicate.*;
import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.compat.bettercombat.BetterCombatCompat;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;
import com.elfmcys.yesstevemodel.client.compat.parcool.ParCoolCompat;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.CodedAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.geo.NativeRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.*;

public abstract class CustomPlayerEntity extends AnimatableEntity<Player> implements IPhysicsEntity {
    private String modelId = ModelIdUtil.DEFAULT_MODEL_ID;
    private String textureName = ModelIdUtil.DEFAULT_TEXTURE_NAME;

    protected final boolean localPlayer;
    protected final PhysicsManager physicsManager;
    protected final PhysicsManager guiPhysicsManager;

    protected boolean isPlayingExtraAnimation = false;
    protected String extraAnimationName = "idle";
    protected boolean isExtraAnimationDirty = false;

    private final Vector2f headRot = new Vector2f();

    private boolean fireInitEvent = false;
    private IValue wrappedUpdateHandler = null;
    private List<IValue> syncHandler = null;

    /**
     * 专为 tacz 枪械事件使用的，用来将枪械动画重置
     */
    private boolean tacGunAnimationNeedReload = false;

    public CustomPlayerEntity(Player player, boolean localPlayer, boolean asyncUpdate) {
        super(player, asyncUpdate);
        this.localPlayer = localPlayer;
        this.physicsManager = new PhysicsManager();
        this.guiPhysicsManager = new PhysicsManager();
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
        addAnimationController(new CodedAnimationController(this, CAP_CONTROLLER, 0.1f, new CapPredicate()));
        if (this instanceof IPreviewEntity) {
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
    public PhysicsManager getPhysicsManager() {
        if (NativeRenderer.isAsyncScope() || RenderUtil.isRenderingEntitiesInPaperDoll()) {
            return physicsManager;
        } else {
            return guiPhysicsManager;
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
    protected boolean updateAnimation(MolangContext ctx, @NotNull AnimationEvent animationEvent) {
        if (animationEvent.getExtraData() != null && entity != null) {
            EntityModelData data = animationEvent.getExtraData();
            this.recoverLastCodedAnimation();
            boolean update = super.updateAnimation(ctx, animationEvent);
            this.codeAnimation(animationEvent, data, update);
            return update;
        } else {
            return super.updateAnimation(ctx, animationEvent);
        }
    }

    /**
     * 现在幂等了
     */
    @Override
    public boolean shouldForceUpdate() {
        return currentFrameRenderTimes > 1 || !NativeRenderer.isAsyncScope();
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
    public boolean isModelPresent() {
        return ClientModelManager.getModels().containsKey(modelId);
    }

    public void setModelAndTexture(String modelId, String textureName) {
        setInitialized();
        this.modelId = modelId;
        this.textureName = textureName;
    }

    public void playExtraAnimation(String animationName) {
        if (ClientModelManager.getPlayerAnimation(getModelId(), animationName).isPresent()) {
            this.extraAnimationName = animationName;
            this.isPlayingExtraAnimation = true;
            this.isExtraAnimationDirty = true;
        } else {
            this.isPlayingExtraAnimation = false;
        }
    }

    public boolean isExtraAnimationDirty() {
        return isExtraAnimationDirty;
    }

    public void clearExtraAnimationDirty() {
        this.isExtraAnimationDirty = false;
    }

    public boolean isPlayingExtraAnimation() {
        return isPlayingExtraAnimation;
    }

    public String getExtraAnimationName() {
        return this.extraAnimationName;
    }

    public void stopExtraAnimation() {
        this.isPlayingExtraAnimation = false;
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
        physicsManager.reset();
    }

    @Override
    protected void preAnimationSetup(float seekTime) {
        // 设置 roaming 变量
        getAnimationProcessor().putRemoteStruct(getRoamingStruct());

        // 更新物理
        getPhysicsManager().update(seekTime);

        // 触发事件
        if (fireInitEvent) {
            fireInitEvent = false;
            var initEvent = getEventHandler(MolangEventWrapper.PLAYER_INIT);
            if (initEvent != null) {
                executeMolangExp(MolangEventWrapper.wrap(initEvent), true, true, null);
            }
        }
        if (wrappedUpdateHandler != null) {
            executeMolangExp(wrappedUpdateHandler, allowEmitting(), true, null);
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
