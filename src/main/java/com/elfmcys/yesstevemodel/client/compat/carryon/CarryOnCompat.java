package com.elfmcys.yesstevemodel.client.compat.carryon;

import com.elfmcys.yesstevemodel.client.animation.AnimationManager;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;

public class CarryOnCompat {
    private static final String CARRY_ON_ID = "carryon";

    public static boolean isCarryOnLoaded() {
        return ModList.get().isLoaded(CARRY_ON_ID);
    }

    public static PlayState predicateCarryOn(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatable().getEntity();
        if (player == null || event.getAnimatable().hasPreviewAnimation()) {
            return PlayState.STOP;
        }
        Type carryOnType = getCarryOnType(player);
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

    public static boolean isCarryOnPrincess(Player player, AnimationEvent<CustomPlayerEntity> event) {
        Entity vehicle = player.getVehicle();
        return vehicle instanceof Player playerVehicle && getCarryOnType(playerVehicle) == Type.PLAYER;
    }

    private static Type getCarryOnType(Player player) {
        if (!isCarryOnLoaded()) {
            return Type.NONE;
        }
        CarryOnData carry = CarryOnDataManager.getCarryData(player);
        if (carry.isCarrying(CarryOnData.CarryType.BLOCK)) {
            return Type.BLOCK;
        }
        if (carry.isCarrying(CarryOnData.CarryType.ENTITY)) {
            return Type.ENTITY;
        }
        if (carry.isCarrying(CarryOnData.CarryType.PLAYER)) {
            return Type.PLAYER;
        }
        return Type.NONE;
    }

    private enum Type {
        ENTITY, BLOCK, PLAYER, NONE
    }
}
