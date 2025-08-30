package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.debug.CustomDebugSource;
import com.elfmcys.yesstevemodel.client.animation.molang.MolangEventWrapper;
import com.elfmcys.yesstevemodel.client.animation.molang.PhysicsManager;
import com.elfmcys.yesstevemodel.client.animation.predicate.*;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.predicate.MaidMiscPredicate;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.predicate.MaidRoulettePredicate;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.predicate.MaidStatuePredicate;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.predicate.YsmMaidMainPredicate;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.entity.IPhysicsEntity;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.CodedAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
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
import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.*;

/**
 * 基于 CustomPlayerEntity 复制来的，基本上没做删除，试想尝试让女仆能调用轮盘动画之类的,所以就先预留着
 */
@OnlyIn(Dist.CLIENT)
public class CustomYsmMaidEntity extends AnimatableEntity<EntityMaid> implements IGeoEntity, IPhysicsEntity {
    private String modelId = ModelIdUtil.DEFAULT_MODEL_ID;
    private String textureName = ModelIdUtil.DEFAULT_TEXTURE_NAME;
    private final Vector2f headRot = new Vector2f();

    private final PhysicsManager physicsManager;
    private final PhysicsManager physicsGuiManager;

    private boolean fireInitEvent = false;
    private IValue wrappedUpdateHandler = null;

    /**
     * 专为 tacz 枪械事件使用的，用来将枪械动画重置
     */
    private boolean tacGunAnimationNeedReload = false;
    private MaidModelInfo maidInfo = new MaidModelInfo();

    public CustomYsmMaidEntity(EntityMaid player, boolean asyncUpdate) {
        super(player, asyncUpdate);
        registerControllers();
        physicsManager = new PhysicsManager();
        physicsGuiManager = new PhysicsManager();
    }

    @Override
    protected MaidStateTracker createStateTracker(EntityMaid entity) {
        return new MaidStateTracker(entity);
    }

    @Override
    public MaidStateTracker getStateTracker() {
        return (MaidStateTracker) super.getStateTracker();
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

        addAnimationController(new HybridAnimationController(this, VEHICLE_CONTROLLER, 0.1f, new VehiclePredicate()));

        addAnimationController(new HybridAnimationController(this, PRE_MAIN_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, MAIN_CONTROLLER, 0.1f, new YsmMaidMainPredicate()));
        addAnimationController(new HybridAnimationController(this, POST_MAIN_CONTROLLER, 0, new EmptyPredicate()));

        addAnimationController(new HybridAnimationController(this, PRE_HOLD_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, HOLD_OFFHAND_CONTROLLER, 0.1f, new OffhandPredicate()));
        addAnimationController(new HybridAnimationController(this, HOLD_MAINHAND_CONTROLLER, 0.1f, new MainhandPredicate()));
        addAnimationController(new HybridAnimationController(this, POST_HOLD_CONTROLLER, 0, new EmptyPredicate()));

        addAnimationController(new HybridAnimationController(this, GUN_FIRE_CONTROLLER, 0f, new GunFirePredicate()));

        addAnimationController(new HybridAnimationController(this, PRE_SWING_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, SWING_CONTROLLER, 0, new SwingPredicate()));
        addAnimationController(new HybridAnimationController(this, POST_SWING_CONTROLLER, 0, new EmptyPredicate()));

        addAnimationController(new HybridAnimationController(this, PRE_USE_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, USE_CONTROLLER, 0.1f, new UsePredicate()));
        addAnimationController(new HybridAnimationController(this, POST_USE_CONTROLLER, 0, new EmptyPredicate()));

        addAnimationController(new HybridAnimationController(this, MAID_MISC, 0.1f, new MaidMiscPredicate()));
        addAnimationController(new HybridAnimationController(this, PASSENGER_CONTROLLER, 0.1f, new PassengerPredicate()));

        // 下面不需要自定义动画控制器
        {
            addAnimationController(new CodedAnimationController(this, CAP_CONTROLLER, 0.1f, new MaidRoulettePredicate()));
        }

        // 高并行动画
        for (int i = 0; i < 8; i++) {
            String controllerName = PARALLEL_CONTROLLER + i;
            String animationName = String.format("parallel%d", i);
            addAnimationController(new HybridAnimationController(this, controllerName, 0,
                    new ParallelPredicate(animationName), true));
        }

        // 护甲动画
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                String controllerName = ARMOR_CONTROLLER + slot.getName();
                addAnimationController(new HybridAnimationController(this, controllerName, 0, new ArmorPredicate(slot)));
            }
        }

        // 雕像动画控制器优先级最高，可以拿来禁用前面所有的动画，避免雕像或者手办还在运动
        addAnimationController(new HybridAnimationController(this, MAID_STATUE, 0, new MaidStatuePredicate()));
    }

    @Override
    public String getModelId() {
        if (isModelPresent()) {
            return modelId;
        }
        return ModelIdUtil.DEFAULT_MODEL_ID;
    }

    public String getTextureName() {
        return textureName;
    }

    @Override
    public float getHeightScale() {
        return ClientModelManager.getModel(modelId).map(model -> model.modelInfo().properties().heightScale()).orElse(0.7f);
    }

    @Override
    public float getWidthScale() {
        return ClientModelManager.getModel(modelId).map(model -> model.modelInfo().properties().widthScale()).orElse(0.7f);
    }

    @Override
    public GeoModel getModel() {
        return ClientModelManager.getModel(modelId).map(ClientModel::mainModel).orElse(ClientModelManager.getDefaultModel().mainModel());
    }

    @Override
    @NotNull
    public Int2ReferenceMap<List<IValue>> getEventHandlers() {
        return ClientModelManager.getModel(modelId)
                .map(m -> (Int2ReferenceMap<List<IValue>>) m.eventHandlers())
                .orElseGet(Int2ReferenceMaps::emptyMap);
    }

    @Nullable
    @Override
    public Animation getAnimation(String name) {
        return ClientModelManager.getPlayerAnimation(modelId, name).orElse(null);
    }

    @Override
    public @Nullable IValue getUserFunction(int name) {
        return ClientModelManager.getUserFunction(modelId, name);
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
            recoverLastCodedAnimation();
            boolean update = super.updateAnimation(ctx, animationEvent);
            this.codeAnimation(animationEvent, data, entity, update);
            return update;
        } else {
            return super.updateAnimation(ctx, animationEvent);
        }
    }

    public boolean isRouletteAnimDirty() {
        return this.entity.rouletteAnimDirty;
    }

    public void clearRouletteAnimDirty() {
        this.entity.rouletteAnimDirty = false;
    }

    public boolean isRouletteAnimPlaying() {
        return this.entity.rouletteAnimPlaying;
    }

    public String getRouletteAnim() {
        return this.entity.rouletteAnim;
    }

    public void setRemoteStruct(Object2FloatOpenHashMap<String> roamingVars) {
        // TODO
    }

    @Override
    public void updateRoamingVars(Object2FloatOpenHashMap<String> roamingVars) {
        // TODO
    }

    public Struct getRemoteStruct() {
        // TODO
        return null;
    }

    @Override
    protected void preAnimationSetup(float seekTime) {
        getAnimationProcessor().putRemoteStruct(getRemoteStruct());

        // 更新物理
        getPhysicsManager().update(seekTime);

        // 触发事件
        // 由于女仆套用了大多数玩家动画，所以要触发玩家事件
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
    private void codeAnimation(AnimationEvent<CustomYsmMaidEntity> animationEvent, EntityModelData data, EntityMaid player, boolean update) {
        var model = getCurrentModel();
        // 更新头部旋转
        if (model != null && !model.headBones().isEmpty()) {
            var head = model.headBones().get(model.headBones().size() - 1);
            if (update) {
                headRot.set(head.getRotationX(), head.getRotationY());
            }
            head.setRotationX(headRot.x + (float) Math.toRadians(data.headPitch));
            head.setRotationY(headRot.y + (float) Math.toRadians(data.netHeadYaw));
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
    protected boolean allowEmitting() {
        // 同一帧内只有第一次更新允许生成行为
        return currentFrameRenderTimes == 1;
    }

    @Override
    public PhysicsManager getPhysicsManager() {
        if (NativeRenderer.isAsyncScope() || RenderUtil.isRenderingEntitiesInPaperDoll()) {
            return physicsManager;
        } else {
            return physicsGuiManager;
        }
    }

    @Override
    public boolean isModelPresent() {
        return ClientModelManager.getModels().containsKey(modelId);
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
        physicsManager.reset();
        physicsGuiManager.reset();
    }

    @Override
    public IMaid getMaid() {
        return this.entity;
    }

    @Override
    public MaidModelInfo getMaidInfo() {
        return maidInfo;
    }

    @Override
    public void setMaidInfo(MaidModelInfo maidModelInfo) {
        if (this.maidInfo != maidModelInfo) {
            this.maidInfo = maidModelInfo;
        }
    }

    @Override
    public ILocationModel getGeoModel() {
        return this.getCurrentModel().getTlmAnimatedGeoModel();
    }

    @Override
    public void setYsmModel(String modelId, String texture) {
        this.modelId = modelId;
        this.textureName = texture;
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
