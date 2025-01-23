package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.AnimationState;
import com.elfmcys.yesstevemodel.client.animation.Priority;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionalVehicle;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;
import com.elfmcys.yesstevemodel.client.compat.parcool.ParCoolCompat;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeCompat;
import com.elfmcys.yesstevemodel.client.compat.swem.SwemCompat;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class PlayerMainPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    @SuppressWarnings("unchecked")
    private static final ReferenceArrayList<AnimationState>[] DATA = new ReferenceArrayList[Priority.LOWEST + 1];

    static {
        for (int i = 0; i < DATA.length; i++) {
            DATA[i] = new ReferenceArrayList<>(6);
        }
    }

    public static void register(AnimationState state) {
        DATA[state.getPriority()].add(state);
    }

    @Override
    public PlayState test(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        Player player = event.getAnimatableEntity().getEntity();
        if (player == null) {
            return PlayState.STOP;
        }
        if (event.getAnimatableEntity().hasPreviewAnimation()) {
            return PlayState.STOP;
        }

        // 跑酷模组兼容
        String parCoolAnimation = ParCoolCompat.getAnimation(player);
        if (parCoolAnimation != null) {
            String modelId = event.getAnimatableEntity().getModelId();
            Optional<Animation> optional = ClientModelManager.getPlayerAnimation(modelId, parCoolAnimation);
            if (optional.isPresent()) {
                return playAnimation(event, parCoolAnimation);
            }
        }

        for (int i = Priority.HIGHEST; i <= Priority.LOWEST; i++) {
            // 载具动画单独检查
            if (i == Priority.HIGH) {
                PlayState vehicleAnimation = getVehicleAnimation(event);
                if (vehicleAnimation != null) {
                    return vehicleAnimation;
                }
            }
            for (AnimationState state : DATA[i]) {
                if (state.getPredicate().test(player, event)) {
                    String animationName = state.getAnimationName();
                    ILoopType loopType = state.getLoopType();

                    // 先判断拔刀剑动画
                    PlayState slashBladeAnimation = SlashBladeCompat.playMainAnimation(player, event, animationName, loopType);
                    if (slashBladeAnimation != null) {
                        return slashBladeAnimation;
                    }

                    // 再判断 tacz 动画
                    PlayState gunMainAnimation = TACZCompat.playGunMainAnimation(player, event, animationName, loopType);
                    return Objects.requireNonNullElseGet(gunMainAnimation, () -> playAnimation(event, animationName, loopType));
                }
            }
        }
        return PlayState.STOP;
    }

    @Nullable
    public PlayState getVehicleAnimation(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatableEntity().getEntity();
        if (player == null || event.getAnimatableEntity().hasPreviewAnimation()) {
            return null;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle == null || !vehicle.isAlive()) {
            return null;
        }

        String swemAnimation = SwemCompat.getAnimation(player);
        if (StringUtils.isNoneBlank(swemAnimation)) {
            return playAnimation(event, swemAnimation, ILoopType.EDefaultLoopTypes.LOOP);
        }

        String id = event.getAnimatableEntity().getModelId();
        ConditionalVehicle vehicleCondition = ClientModelManager.getModel(id).map(model -> model.conditionManager().getVehicle()).orElse(null);
        if (vehicleCondition != null) {
            String name = vehicleCondition.doTest(player);
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
        if (CarryOnCompat.isCarryOnPrincess(player, event)) {
            return playAnimation(event, "carryon:princess", ILoopType.EDefaultLoopTypes.LOOP);
        }
        return playAnimation(event, "sit", ILoopType.EDefaultLoopTypes.LOOP);
    }
}
