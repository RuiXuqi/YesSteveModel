package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.predicate.ParallelPredicate;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.world.entity.player.Player;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.FP_ARM_PARALLEL_CONTROLLER;

public class CustomFirstPersonArmEntity extends CustomPlayerEntity {
    public CustomFirstPersonArmEntity(Player player, boolean localPlayer, boolean asyncUpdate) {
        super(player, localPlayer, asyncUpdate);
    }

    @Override
    @SuppressWarnings("all")
    public void registerControllers() {
        for (int i = 0; i < 8; i++) {
            String controllerName = FP_ARM_PARALLEL_CONTROLLER + i;
            String animationName = String.format("fp_arm_parallel%d", i);
            addAnimationController(new HybridAnimationController(this, controllerName, 0,
                    new ParallelPredicate(animationName), true));
        }
    }

    @Override
    public GeoModel getModel() {
        return ClientModelManager.getModel(modelId)
                .map(ClientModel::armModel)
                .orElse(ClientModelManager.getDefaultModel().armModel());
    }

    @Override
    protected boolean allowEmitting() {
        return false;
    }
}
