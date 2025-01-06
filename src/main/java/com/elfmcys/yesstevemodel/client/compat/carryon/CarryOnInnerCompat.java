package com.elfmcys.yesstevemodel.client.compat.carryon;

import com.elfmcys.yesstevemodel.client.animation.AnimationManager;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;

public class CarryOnInnerCompat {
    static PlayState predicateCarryOn(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatableEntity().getEntity();
        if (player == null || event.getAnimatableEntity().hasPreviewAnimation()) {
            return PlayState.STOP;
        }
        CarryOnInnerCompat.Type carryOnType = getCarryOnType(player);
        switch (carryOnType) {
            case ENTITY -> {
                return AnimationManager.playLoopAnimation(event, "carryon:entity");
            }
            case BLOCK -> {
                return AnimationManager.playLoopAnimation(event, "carryon:block");
            }
            case PLAYER -> {
                return AnimationManager.playLoopAnimation(event, "carryon:player");
            }
        }
        return PlayState.STOP;
    }

    static boolean isCarryOnPrincess(Player player) {
        Entity vehicle = player.getVehicle();
        return vehicle instanceof Player playerVehicle && getCarryOnType(playerVehicle) == CarryOnInnerCompat.Type.PLAYER;
    }

    static CarryOnInnerCompat.Type getCarryOnType(Player player) {
        CarryOnData carry = CarryOnDataManager.getCarryData(player);
        if (carry.isCarrying(CarryOnData.CarryType.BLOCK)) {
            return CarryOnInnerCompat.Type.BLOCK;
        }
        if (carry.isCarrying(CarryOnData.CarryType.ENTITY)) {
            return CarryOnInnerCompat.Type.ENTITY;
        }
        if (carry.isCarrying(CarryOnData.CarryType.PLAYER)) {
            return CarryOnInnerCompat.Type.PLAYER;
        }
        return CarryOnInnerCompat.Type.NONE;
    }

    enum Type {
        ENTITY, BLOCK, PLAYER, NONE
    }
}
