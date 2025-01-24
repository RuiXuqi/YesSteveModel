package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.animation.AnimationState;
import com.elfmcys.yesstevemodel.client.animation.Priority;
import com.elfmcys.yesstevemodel.client.compat.parcool.ParCoolCompat;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeCompat;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

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
        boolean parcool = ParCoolCompat.hasAnimation(player);
        if (parcool) {
            return PlayState.STOP;
        }
        // 载具动画单独检查
        Entity vehicle = player.getVehicle();
        if (vehicle != null && vehicle.isAlive()) {
            return PlayState.STOP;
        }

        for (int i = Priority.HIGHEST; i <= Priority.LOWEST; i++) {
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
}
