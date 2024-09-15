package com.elfmcys.yesstevemodel.client.animation.controller;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.AnimationBuilder;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationControllerState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.tuple.Pair;

public final class NewAnimationManager {
    public static PlayState predicate(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<AnimationContext<?>> evaluator, GeoAnimationController controllerData) {
        if (Minecraft.getInstance().isPaused()) {
            return PlayState.STOP;
        }
        var controller = event.getController();
        // 先尝试获取当前的状态
        String stateName = controller.getStateName();
        // 如果当前状态为 null，装入初始化状态
        if (stateName == null) {
            controller.setStateName(controllerData.initialState());
            return PlayState.STOP;
        }
        // 如果当前状态不存在，装入初始化状态
        var stateMap = controllerData.states();
        if (!stateMap.containsKey(stateName)) {
            controller.setStateName(controllerData.initialState());
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
                controller.setAnimation(new AnimationBuilder().addAnimation(name));
                break;
            }
        }

        // 检查状态机，是否需要切换下一个状态
        for (Pair<String, IValue> transitions : state.transitions()) {
            String name = transitions.getLeft();
            IValue condition = transitions.getRight();
            boolean canSwitch = condition == null || condition.evalAsBoolean(evaluator);
            if (canSwitch) {
                controller.setStateName(name);
                controller.transitionLengthTicks = state.blendTransition() * 20;
                break;
            }
        }

        return PlayState.CONTINUE;
    }
}
