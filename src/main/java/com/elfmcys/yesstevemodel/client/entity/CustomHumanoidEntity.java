package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.elfmcys.yesstevemodel.client.animation.molang.MolangEventWrapper;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

/**
 * 使用玩家模型的实体
 */
public abstract class CustomHumanoidEntity<T extends LivingEntity> extends CustomEntity<T> {
    private String textureName;
    private ResourceLocation textureLocation;
    private int textureIndex;

    private final Vector2f headRot = new Vector2f();
    private boolean fireInitEvent = false;
    private IValue wrappedUpdateHandler = null;

    /**
     * 专为 tacz 枪械事件使用的，用来将枪械动画重置
     */
    private boolean tacGunAnimationNeedReload = false;

    protected CustomHumanoidEntity(T entity, boolean asyncUpdate) {
        super(entity, asyncUpdate);
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

    @Deprecated
    protected void codeAnimation(AnimationEvent<CustomPlayerEntity> animationEvent, EntityModelData data, boolean update) {
        GeoModelState model = getLoadedGeoModel();
        if (model != null && !model.headBones().isEmpty()) {
            var head = model.headBones().get(model.headBones().size() - 1);
            // 更新头部旋转
            if (update) {
                headRot.set(head.getRotationX(), head.getRotationY());
            }
            head.setRotationX(headRot.x + (float) Math.toRadians(data.headPitch));
            head.setRotationY(headRot.y + (float) Math.toRadians(data.netHeadYaw));
        }
    }

    protected void recoverLastCodedAnimation() {
        var model = getLoadedGeoModel();
        if (model != null && !model.headBones().isEmpty()) {
            var head = model.headBones().get(model.headBones().size() - 1);
            head.setRotationX(headRot.x);
            head.setRotationY(headRot.y);
        }
    }

    @Override
    protected HumanoidStateTracker<T> createStateTracker(T entity) {
        return new HumanoidStateTracker<T>(entity);
    }

    @Override
    public HumanoidStateTracker<T> getStateTracker() {
        return (HumanoidStateTracker<T>) super.getStateTracker();
    }

    public void updateTextureName(String textureName) {
        this.textureName = textureName;
        updateTexture();
    }

    public void updateModelAndTexture(String modelId, String textureName) {
        setInitialized();
        this.textureName = textureName;
        updateModelId(modelId);
    }

    @Override
    protected boolean onLoadModelContainer(ClientModel newModel, boolean isFallback) {
        updateTexture();
        return true;
    }

    @Override
    protected void onLoadGeoModel(GeoModelState model) {
        if (model != null && !model.headBones().isEmpty()) {
            var head = model.headBones().get(model.headBones().size() - 1);
            headRot.set(head.getRotationX(), head.getRotationY());
        }

        fireInitEvent = true;
        var updateHandlers = getEventHandler(MolangEventWrapper.PLAYER_UPDATE);
        if (updateHandlers != null) {
            wrappedUpdateHandler = MolangEventWrapper.wrap(updateHandlers);
        } else {
            wrappedUpdateHandler = null;
        }
        physicsManager.reset();
    }

    @Override
    protected void preAnimationSetup(float seekTime) {
        super.preAnimationSetup(seekTime);
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

    public ConditionManager getConditionManager() {
        return getModelContainer().playerModel().conditionManager();
    }

    private void updateTexture() {
        var textures = getModelContainer().playerModel().textures();
        var texture = textures.get(textureName);
        if (texture != null) {
            this.textureLocation = texture;
            this.textureIndex = textures.valueList().indexOf(texture);
        } else if (isModelPresent()) {
            this.textureName = textures.getKeyAt(0);
            this.textureLocation = textures.getValueAt(0);
            this.textureIndex = 0;
        }
    }

    @Override
    public GeoModel getYsmGeoModel() {
        return getModelContainer().playerModel().mainModel();
    }

    @Nullable
    @Override
    public Animation getAnimation(String name) {
        return getModelContainer().playerModel().animations().get(name);
    }

    @Nullable
    @Override
    public AnimationControllerData getAnimationControllerData(String animationControllerName) {
        return getModelContainer().playerModel().animationControllers().get(animationControllerName);
    }

    public String getTextureName() {
        return isModelPresent() ? textureName : getModelContainer().playerModel().textures().getKeyAt(0);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation() {
        return isModelPresent() ? textureLocation : getModelContainer().playerModel().textures().getValueAt(0);
    }

    @Override
    public int getTextureIndex() {
        return isModelPresent() ? textureIndex : 0;
    }

    @Override
    public float getHeightScale() {
        return getModelContainer().modelInfo().properties().heightScale();
    }

    @Override
    public float getWidthScale() {
        return getModelContainer().modelInfo().properties().widthScale();
    }

    public boolean renderLayersFirst() {
        return getModelContainer().modelInfo().properties().renderLayersFirst();
    }

    public boolean isTacGunAnimationNeedReload() {
        return tacGunAnimationNeedReload;
    }

    public void setTacGunAnimationNeedReload(boolean tacGunAnimationNeedReload) {
        this.tacGunAnimationNeedReload = tacGunAnimationNeedReload;
    }
}
