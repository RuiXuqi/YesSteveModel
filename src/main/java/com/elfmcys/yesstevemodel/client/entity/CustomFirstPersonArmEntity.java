package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapability;
import com.elfmcys.yesstevemodel.client.animation.predicate.ParallelPredicate;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.FP_ARM_PARALLEL_CONTROLLER;

public class CustomFirstPersonArmEntity extends CustomEntity<LocalPlayer> {
    private final PlayerAnimatableCapability mainModelEntity;

    public CustomFirstPersonArmEntity(LocalPlayer player, PlayerAnimatableCapability mainModelEntity) {
        super(player, false);
        this.mainModelEntity = mainModelEntity;
        registerControllers();
        updateModelId(mainModelEntity.getModelId());
    }

    public PlayerAnimatableCapability getMainModelEntity() {
        return mainModelEntity;
    }

    @SuppressWarnings("all")
    private void registerControllers() {
        for (int i = 0; i < 8; i++) {
            String controllerName = FP_ARM_PARALLEL_CONTROLLER + i;
            String animationName = String.format("fp_arm_parallel%d", i);
            addAnimationController(new HybridAnimationController(this, controllerName, 0,
                    new ParallelPredicate(animationName), true));
        }
    }

    @Override
    protected boolean prepareForUpdate() {
        if (mainModelEntity.getModelContainer() != getModelContainer()) {
            updateModelId(mainModelEntity.getModelId());
            return true;
        }
        return super.prepareForUpdate();
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return mainModelEntity.getTextureLocation();
    }

    @Override
    public float getWidthScale() {
        return getModelContainer().modelInfo().properties().widthScale();
    }

    @Override
    public float getHeightScale() {
        return getModelContainer().modelInfo().properties().heightScale();
    }

    @Override
    public @Nullable Animation getAnimation(String name) {
        return getModelContainer().playerModel().animations().get(name);
    }

    @Override
    protected GeoModel getYsmGeoModel() {
        return getModelContainer().playerModel().armModel();
    }

    @Override
    protected void preAnimationSetup(float seekTime) {
        // 设置 roaming 变量
        getAnimationProcessor().putRemoteStruct(mainModelEntity.getRoamingStruct());
    }
}
