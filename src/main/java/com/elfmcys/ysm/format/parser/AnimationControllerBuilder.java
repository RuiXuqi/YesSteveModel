package com.elfmcys.ysm.format.parser;

import com.elfmcys.ysm.format.parser.pojo.controller.AnimationController;
import com.elfmcys.ysm.format.parser.pojo.controller.AnimationControllerFile;
import com.elfmcys.ysm.format.parser.pojo.controller.State;
import mixel.common.StringPairOuterClass;
import mixel.asset.model.data.AnimationControllerOuterClass;

import java.util.Objects;

public class AnimationControllerBuilder {
    private AnimationControllerBuilder() {
    }

    public static AnimationControllerOuterClass.AnimationControllerFile build(AnimationControllerFile pojo) {
        var result = AnimationControllerOuterClass.AnimationControllerFile.newInstance();
        for (var entry : pojo.animationControllers.entrySet()) {
            result.addControllers(toProto(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private static AnimationControllerOuterClass.AnimationController toProto(
            String name, AnimationController src) {
        var dst = AnimationControllerOuterClass.AnimationController.newInstance()
                .setName(name)
                .setDefaultState(Objects.requireNonNullElse(src.initialState, "default"));
        for (var state : src.states.entrySet()) {
            dst.addStates(toProto(state.getKey(), state.getValue()));
        }
        return dst;
    }

    private static AnimationControllerOuterClass.State toProto(
            String name, State src) {
        var dst = AnimationControllerOuterClass.State.newInstance()
                .setName(name)
                .setBlendViaShortestPath(src.blendViaShortestPath);
        for (var animation : src.animations) {
            dst.addAnimations(StringPairOuterClass.StringPair.newInstance()
                    .setKey(Objects.requireNonNullElse(animation.name, ""))
                    .setValue(Objects.requireNonNullElse(animation.condition, "")));
        }
        for (var transition : src.transitions) {
            dst.addTransitions(AnimationControllerOuterClass.Transition.newInstance()
                    .setDst(transition.destStateName)
                    .setCondition(transition.condition));
        }
        for (var entry : src.onEntry) {
            dst.addOnEntry(entry);
        }
        for (var exit : src.onExit) {
            dst.addOnExit(exit);
        }
        for (var sound : src.soundEffects) {
            dst.addSoundEffects(sound.effect);
        }
        if (src.blendTransition != null) {
            var blend = dst.getMutableBlendTransition();
            if (src.blendTransition.linearLength != null) {
                blend.setLinearLength(src.blendTransition.linearLength);
            } else {
                for (var point : src.blendTransition.points.entrySet()) {
                    blend.addPoints(AnimationControllerOuterClass.BlendPoint.newInstance()
                            .setKey(point.getKey())
                            .setValue(point.getValue()));
                }
            }
        }
        return dst;
    }
}
