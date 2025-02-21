package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionalChair;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionalVehicle;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;
import com.elfmcys.yesstevemodel.client.compat.swem.SwemCompat;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TlmCompat;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class VehiclePredicate implements IAnimationPredicate<AnimatableEntity<? extends LivingEntity>> {
    @Override
    public PlayState test(AnimationEvent<AnimatableEntity<? extends LivingEntity>> event, ExpressionEvaluator<?> evaluator) {
        PlayState vehicleAnimation = getVehicleAnimation(event);
        return Objects.requireNonNullElse(vehicleAnimation, PlayState.STOP);
    }

    @Nullable
    public PlayState getVehicleAnimation(AnimationEvent<AnimatableEntity<? extends LivingEntity>> event) {
        LivingEntity entity = event.getAnimatableEntity().getEntity();
        if (entity == null || event.getAnimatableEntity().hasPreviewAnimation()) {
            return null;
        }
        Entity vehicle = entity.getVehicle();
        if (vehicle == null || !vehicle.isAlive()) {
            return null;
        }

        String swemAnimation = SwemCompat.getAnimation(entity);
        if (StringUtils.isNoneBlank(swemAnimation)) {
            return playAnimation(event, swemAnimation, ILoopType.EDefaultLoopTypes.LOOP);
        }

        String id = event.getAnimatableEntity().getModelId();
        Optional<ClientModel> clientModel = ClientModelManager.getModel(id);

        // 优先判断 chair
        if (TlmCompat.isInstalled()) {
            ConditionalChair conditionalChair = clientModel.map(model -> model.conditionManager().getChair()).orElse(null);
            if (conditionalChair != null) {
                String name = conditionalChair.doTest(entity);
                if (StringUtils.isNoneBlank(name)) {
                    return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                }
            }
        }

        // 然后才是普通载具
        ConditionalVehicle vehicleCondition = clientModel.map(model -> model.conditionManager().getVehicle()).orElse(null);
        if (vehicleCondition != null) {
            String name = vehicleCondition.doTest(entity);
            if (StringUtils.isNoneBlank(name)) {
                return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
            }
        }

        // 兼容旧版本的几个动画
        if (vehicle instanceof Pig) {
            return playAnimation(event, "ride_pig", ILoopType.EDefaultLoopTypes.LOOP);
        }
        if (vehicle instanceof Saddleable) {
            return playAnimation(event, "ride", ILoopType.EDefaultLoopTypes.LOOP);
        }
        if (vehicle instanceof Boat) {
            return playAnimation(event, "boat", ILoopType.EDefaultLoopTypes.LOOP);
        }

        // carry on 兼容
        boolean playerIsOnPrincess = entity instanceof Player player && CarryOnCompat.isCarryOnPrincess(player);
        boolean maidIsOnPrincess = TlmCompat.isMaid(entity) && entity.getVehicle() instanceof Player;
        if (playerIsOnPrincess || maidIsOnPrincess) {
            return playAnimation(event, "carryon:princess", ILoopType.EDefaultLoopTypes.LOOP);
        }

        // 如果是女仆，那么需要兼容几个女仆的内容
        if (TlmCompat.isMaid(entity)) {
            return TlmCompat.getMaidVehicleAnimation(event, entity, vehicle);
        }

        return playAnimation(event, "sit", ILoopType.EDefaultLoopTypes.LOOP);
    }
}
