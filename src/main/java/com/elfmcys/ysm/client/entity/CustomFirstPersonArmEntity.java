package com.elfmcys.ysm.client.entity;

import com.elfmcys.ysm.capability.PlayerAnimatableCapability;
import com.elfmcys.ysm.client.animation.condition.FPArmConditionManager;
import com.elfmcys.ysm.client.model.ClientModel;
import com.elfmcys.ysm.geckolib3.core.builder.Animation;
import com.elfmcys.ysm.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.ysm.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class CustomFirstPersonArmEntity extends CustomEntity<LocalPlayer> {
    private final PlayerAnimatableCapability mainModelEntity;

    public CustomFirstPersonArmEntity(LocalPlayer player, PlayerAnimatableCapability mainModelEntity) {
        super(player, false);
        this.mainModelEntity = mainModelEntity;
        updateModelId(mainModelEntity.getModelId());
    }

    @Override
    protected void onSetupAnimationController() {
        getModelContainer().playerModel().fpArmControllerFactory().accept(this);
    }

    public PlayerAnimatableCapability getMainModelEntity() {
        return mainModelEntity;
    }

    @Override
    protected boolean isImmutableRender(AnimationEvent<?> animEvent) {
        return true;
    }

    @Override
    public void checkModelUpdate() {
        if (mainModelEntity.getModelContainer() != getModelContainer()) {
            updateModelId(mainModelEntity.getModelId());
        }
    }

    @Override
    protected @Nullable ResourceHolder createResourceHolder(ClientModel model, boolean isFallback) {
        return mainModelEntity.getResourceHolder();
    }

    @Override
    public @Nullable AnimationControllerData getAnimationControllerData(String animationControllerName) {
        return getModelContainer().playerModel().animationControllers().get(animationControllerName);
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return mainModelEntity.getTextureLocation();
    }

    @Override
    public float getWidthScale() {
        return getModelContainer().info().properties().widthScale();
    }

    @Override
    public float getHeightScale() {
        return getModelContainer().info().properties().heightScale();
    }

    @Override
    public @Nullable Animation getAnimation(String name) {
        return getModelContainer().playerModel().fpArmAnimations().get(name);
    }

    public FPArmConditionManager getFPArmConditionManager() {
        return getModelContainer().playerModel().fpArmConditionManager();
    }

    @Override
    protected GeoModel getYsmGeoModel() {
        return getModelContainer().playerModel().armModel();
    }

    @Override
    protected void preAnimationSetup(float seekTime, boolean shouldTick) {
        // 设置 roaming 变量
        getAnimationProcessor().putRemoteStruct(mainModelEntity.getRoamingStruct());
    }
}
