package com.elfmcys.yesstevemodel.client.animation.controller;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.AnimationBuilder;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationControllerState;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class NewAnimationManager {
    public static PlayState predicate(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<AnimationContext<?>> evaluator, AnimationController.IAnimationPredicate<CustomPlayerEntity> oldPredicate) {
        return predicate(event, evaluator, (e) -> {
        }, oldPredicate);
    }

    public static PlayState predicate(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<AnimationContext<?>> evaluator,
                                      Consumer<AnimationEvent<CustomPlayerEntity>> extra,
                                      AnimationController.IAnimationPredicate<CustomPlayerEntity> oldPredicate) {
        var controller = event.getController();
        String controllerName = controller.getName();
        String modelId = event.getAnimatable().getModelId();
        return ClientModelManager.getModel(modelId).map(clientModel -> {
            var controllers = clientModel.animationControllers();
            // 如果动画控制器不存在，那么使用旧版本动画
            if (controllers.containsKey(controllerName)) {
                extra.accept(event);
                return NewAnimationManager.predicate(event, evaluator, controllers.get(controllerName));
            }
            // 旧版动画需要重置一下过渡
            controller.transitionLengthTicks = controller.initTransitionLengthTicks;
            return oldPredicate.test(event, evaluator);
        }).orElse(PlayState.STOP);
    }

    private static PlayState predicate(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<AnimationContext<?>> evaluator, GeoAnimationController controllerData) {
        Player player = event.getAnimatable().getEntity();
        if (player == null) {
            return PlayState.STOP;
        }
        if (event.getAnimatable().hasPreviewAnimation()) {
            return PlayState.STOP;
        }
        if (Minecraft.getInstance().isPaused()) {
            return PlayState.STOP;
        }
        var controller = event.getController();
        var stateMap = controllerData.states();
        // 先尝试获取当前的状态
        String stateName = controller.getStateName();
        String initialStateName = controllerData.initialState();
        // 如果当前状态为 null 或者不存在，装入初始化状态
        if (StringUtils.isBlank(stateName) || !stateMap.containsKey(stateName)) {
            switchState(evaluator, stateMap, initialStateName, controller);
            return PlayState.STOP;
        }

        // 开始播放该状态动画
        GeoAnimationControllerState state = stateMap.get(stateName);
        // FIXME: 目前还不支持多个动画同时播放
        // 先暂时播放单个动画试试
        for (var action : state.animations()) {
            String name = action.getLeft();
            IValue condition = action.getRight();
            boolean canPlay = condition == null || condition.evalAsBoolean(evaluator);
            if (canPlay) {
                // 强制把 transitionLength 修改为当前数值，防止重载时候失效
                controller.transitionLengthTicks = state.blendTransition() * 20;
                controller.setAnimation(new AnimationBuilder().addAnimation(name));
                break;
            }
        }

        // 检查状态机，是否需要切换下一个状态
        for (Pair<String, IValue> transitions : state.transitions()) {
            String nextName = transitions.getLeft();
            IValue condition = transitions.getRight();
            boolean canSwitch = condition == null || condition.evalAsBoolean(evaluator);
            if (canSwitch) {
                // 上一个退出的先执行
                List<IValue> values = state.onExit();
                if (values != null && !values.isEmpty()) {
                    values.forEach(v -> v.evalAsBoolean(evaluator));
                }
                // 下一个将要执行的
                switchState(evaluator, stateMap, nextName, controller);
                break;
            }
        }

        return PlayState.CONTINUE;
    }

    private static void switchState(ExpressionEvaluator<AnimationContext<?>> evaluator, Map<String, GeoAnimationControllerState> stateMap, String nextStateName, AnimationController<CustomPlayerEntity> controller) {
        GeoAnimationControllerState nextState = stateMap.get(nextStateName);
        if (nextState != null) {
            controller.setStateName(nextStateName);
            controller.transitionLengthTicks = nextState.blendTransition() * 20;
            List<IValue> entry = nextState.onEntry();
            if (entry != null && !entry.isEmpty()) {
                entry.forEach(v -> v.evalAsBoolean(evaluator));
            }
        }
    }
}
