package com.elfmcys.ysm.client.entity;

import com.elfmcys.ysm.client.ClientModelManager;
import com.elfmcys.ysm.client.animation.condition.ConditionManager;
import com.elfmcys.ysm.client.animation.molang.MolangEventWrapper;
import com.elfmcys.ysm.client.model.ClientModel;
import com.elfmcys.ysm.client.texture.CustomTextureManager;
import com.elfmcys.ysm.client.texture.TextureHolder;
import com.elfmcys.ysm.geckolib3.core.builder.Animation;
import com.elfmcys.ysm.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.ysm.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.ysm.geckolib3.core.molang.value.IValue;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.ysm.geckolib3.model.AnimatableEntity;
import com.elfmcys.ysm.geckolib3.model.GeoModelState;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用玩家模型的实体
 */
public abstract class CustomHumanoidEntity<T extends LivingEntity> extends CustomEntity<T> {
    protected String textureName;
    private int textureIndex;

    private final Vector2f headRot = new Vector2f();
    private boolean fireInitEvent = false;
    private IValue wrappedUpdateHandler = null;
    private final BooleanList updateHandlerArgs = new BooleanArrayList(1);

    /**
     * 用于禁用 YSM 模型，因为有玩家想强制显示原版玩家模型
     */
    private boolean disabled = false;

    /**
     * 专为 tacz 枪械事件使用的，用来将枪械动画重置
     */
    private boolean tacGunAnimationNeedReload = false;

    protected CustomHumanoidEntity(T entity, boolean asyncUpdate) {
        super(entity, asyncUpdate);
        updateHandlerArgs.size(1);
    }

    @Override
    protected void codeAnimation(AnimationEvent<? extends AnimatableEntity<T>> animationEvent, boolean shouldUpdate) {
        GeoModelState model = getLoadedGeoModel();
        if (model != null && !model.headBones().isEmpty()) {
            var head = model.headBones().get(model.headBones().size() - 1);
            // 更新头部旋转
            if (shouldUpdate) {
                headRot.set(head.getRotationX(), head.getRotationY());
            }
            var data = animationEvent.getExtraData();
            head.setRotationX(headRot.x + (float) Math.toRadians(data.headPitch));
            head.setRotationY(headRot.y + (float) Math.toRadians(data.netHeadYaw));
        }
    }

    @Override
    protected void recoverLastCodedAnimation(boolean lastFrameUpdated) {
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
        updateTexture();
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isInitializedAndEnabled() {
        return isInitialized() && !disabled;
    }

    @Override
    protected void onLoadModelContainer(ClientModel newModel) {
        super.onLoadModelContainer(newModel);
        updateTexture();
        var updateHandlers = newModel.assets().eventHandlers().get(MolangEventWrapper.PLAYER_UPDATE);
        if (updateHandlers != null) {
            wrappedUpdateHandler = MolangEventWrapper.wrap(updateHandlers, updateHandlerArgs);
        } else {
            wrappedUpdateHandler = null;
        }
    }

    @Override
    protected void resetModelContainer() {
        super.resetModelContainer();
        wrappedUpdateHandler = null;
    }

    @Override
    protected void onLoadGeoModel(GeoModelState model) {
        super.onLoadGeoModel(model);
        if (model != null && !model.headBones().isEmpty()) {
            var head = model.headBones().get(model.headBones().size() - 1);
            headRot.set(head.getRotationX(), head.getRotationY());
        }
    }

    @Override
    protected void resetGeoModel() {
        super.resetGeoModel();
        headRot.set(0);
        tacGunAnimationNeedReload = false;
        fireInitEvent = true;
    }

    @Override
    public void reset() {
        super.reset();
        textureName = null;
        textureIndex = 0;
        disabled = false;
    }

    @Override
    protected void preAnimationSetup(float seekTime, boolean shouldTick) {
        super.preAnimationSetup(seekTime, shouldTick);
        if (fireInitEvent) {
            fireInitEvent = false;
            var initEvent = getEventHandler(MolangEventWrapper.PLAYER_INIT);
            if (initEvent != null) {
                executeMolangExp(MolangEventWrapper.wrap(initEvent), true, true, null);
            }
        }
        if (wrappedUpdateHandler != null) {
            updateHandlerArgs.set(0, shouldTick);
            executeMolangExp(wrappedUpdateHandler, true, true, null);
        }
    }

    public ConditionManager getConditionManager() {
        return getModelContainer().playerModel().conditionManager();
    }

    @SuppressWarnings("unchecked")
    private void updateTexture() {
        if (isModelPresent()) {
            var textures = getModelContainer().playerModel().textures();
            var texture = textures.get(textureName);
            if (texture != null) {
                ((HumanoidResourceHolder) getResourceHolder()).setTexture(texture);
                this.textureIndex = textures.valueList().indexOf(texture);
            } else {
                this.textureName = textures.getKeyAt(0);
                ((HumanoidResourceHolder) getResourceHolder()).setTexture(textures.getValueAt(0));
                this.textureIndex = 0;
            }
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

    @Override
    public @Nullable AnimationControllerData getAnimationControllerData(String name) {
        return getModelContainer().playerModel().animationControllers().get(name);
    }

    public String getTextureName() {
        return isModelPresent() ? textureName : getModelContainer().playerModel().textures().getKeyAt(0);
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public ResourceLocation getTextureLocation() {
        return isModelPresent() ? ((HumanoidResourceHolder) getResourceHolder()).textureHolder.id().get() : ClientModelManager.getDefaultModelTextureId();
    }

    @Override
    public int getTextureIndex() {
        return isModelPresent() ? textureIndex : 0;
    }

    @Override
    public float getHeightScale() {
        return getModelContainer().info().properties().heightScale();
    }

    @Override
    public float getWidthScale() {
        return getModelContainer().info().properties().widthScale();
    }

    public boolean renderLayersFirst() {
        return getModelContainer().info().properties().renderLayersFirst();
    }

    public boolean isTacGunAnimationNeedReload() {
        return tacGunAnimationNeedReload;
    }

    public void setTacGunAnimationNeedReload(boolean tacGunAnimationNeedReload) {
        this.tacGunAnimationNeedReload = tacGunAnimationNeedReload;
    }

    protected class HumanoidResourceHolder extends ResourceHolder {
        public TextureHolder textureHolder;
        private final List<TextureHolder> textureHolders;
        private final int releaseDelay;

        public HumanoidResourceHolder(ClientModel model, boolean fallback, boolean registerAllTexture, boolean immediately, int releaseDelay) {
            super(model, fallback);
            var selectedTexture = model.playerModel().textures().get(textureName);
            this.textureHolder = CustomTextureManager.register(selectedTexture != null ? selectedTexture : model.playerModel().defaultTexture(), immediately, releaseDelay);
            this.releaseDelay = releaseDelay;
            if (registerAllTexture) {
                textureHolders = new ArrayList<>();
                for (var texture : model.playerModel().textures().values()) {
                    textureHolders.add(CustomTextureManager.register(texture, false));
                }
                for (var projectile : model.projectileModels().values()) {
                    textureHolders.add(CustomTextureManager.register(projectile.texture(), false));
                }
                for (var vehicle : model.vehicleModels().values()) {
                    textureHolders.add(CustomTextureManager.register(vehicle.texture(), false));
                }
            } else {
                textureHolders = null;
            }
        }

        private void setTexture(AbstractTexture texture) {
            textureHolder = CustomTextureManager.register(texture, true, releaseDelay);
        }

        @Override
        public boolean isLoaded() {
            return textureHolder.id().isPresent();
        }
    }
}
