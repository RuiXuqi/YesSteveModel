package com.elfmcys.yesstevemodel.client.compat.slashblade;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.init.ModItemTags;
import mods.flammpfeil.slashblade.capability.slashblade.CapabilitySlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SlashBladeAnimation {
    static boolean isSlashBlade(ItemStack stack) {
        return stack.getItem() instanceof ItemSlashBlade || stack.is(ModItemTags.SLASH_BLADE);
    }

    static String getAnimationName(AnimationEvent<? extends AnimatableEntity<? extends LivingEntity>> event) {
        LivingEntity livingEntity = event.getAnimatableEntity().getEntity();
        return getCombName(livingEntity.getMainHandItem(), livingEntity.level());
    }

    static String getAnimationName(IContext<? extends LivingEntity> context) {
        LivingEntity livingEntity = context.entity();
        return getCombName(livingEntity.getMainHandItem(), livingEntity.level());
    }

    /**
     * slashblade:idle
     * slashblade:run
     * slashblade:walk
     */
    static PlayState playMainAnimation(AnimationEvent<? extends AnimatableEntity<? extends LivingEntity>> event, String animationName, ILoopType loopType) {
        String name = "slashblade:" + animationName;
        String modelId = event.getAnimatableEntity().getModelId();
        Optional<Animation> playerAnimation = ClientModelManager.getPlayerAnimation(modelId, name);
        if (playerAnimation.isPresent()) {
            return playAnimation(event, name, loopType);
        }
        return playAnimation(event, animationName, loopType);
    }

    @NotNull
    private static String getCombName(ItemStack mainHandItem, Level level) {
        if (!SlashBladeCompat.isSlashBladeItem(mainHandItem)) {
            return StringUtils.EMPTY;
        }
        return mainHandItem.getCapability(CapabilitySlashBlade.BLADESTATE).map(bladeState -> {
            long time = (level.getGameTime() - bladeState.getLastActionTime()) * 50;
            if (SlashBladeCompat.isResharped()) {
                // 重锋兼容
                return SlashBladeResharped.getResharpedComboStateName(bladeState, time);
            } else if (bladeState instanceof SlashBladeState slashBladeState) {
                // 旧版拔刀兼容
                return SlashBladeUnsafe.getOldComboStateName(slashBladeState, time);
            }
            return StringUtils.EMPTY;
        }).orElse(StringUtils.EMPTY);
    }

    @NotNull
    private static PlayState playAnimation(AnimationEvent<?> event, String animationName, ILoopType loopType) {
        event.getCodedController().setAnimation(animationName, loopType);
        return PlayState.CONTINUE;
    }
}
