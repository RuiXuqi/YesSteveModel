package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapability;
import com.elfmcys.yesstevemodel.client.animation.condition.FPArmConditionManager;
import com.elfmcys.yesstevemodel.client.animation.predicate.EmptyPredicate;
import com.elfmcys.yesstevemodel.client.animation.predicate.FPArmArmorPredicate;
import com.elfmcys.yesstevemodel.client.animation.predicate.ParallelPredicate;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.*;

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

    @Override
    protected boolean isImmutableRender() {
        return true;
    }

    @SuppressWarnings("all")
    private void registerControllers() {
        addAnimationController(new HybridAnimationController(this, FP_ARM_MISC_CONTROLLER, 0, new EmptyPredicate()));
        for (int i = 0; i < 8; i++) {
            String controllerName = FP_ARM_PARALLEL_CONTROLLER + i;
            String animationName = String.format("parallel%d", i);
            addAnimationController(new HybridAnimationController(this, controllerName, 0,
                    new ParallelPredicate(animationName), true));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                String controllerName = FP_ARM_ARMOR_CONTROLLER + slot.getName();
                addAnimationController(new HybridAnimationController(this, controllerName, 0, new FPArmArmorPredicate(slot)));
            }
        }
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
    protected void preAnimationSetup(float seekTime) {
        // 设置 roaming 变量
        getAnimationProcessor().putRemoteStruct(mainModelEntity.getRoamingStruct());
    }
}
