package com.elfmcys.ysm.client.model.internal.render;

import com.elfmcys.ysm.client.animation.molang.CustomMolangParser;
import com.elfmcys.ysm.geckolib3.core.builder.Animation;
import com.elfmcys.ysm.geckolib3.core.builder.LoopType;
import com.elfmcys.ysm.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.ysm.geckolib3.core.builder.controller.AnimationControllerState;
import com.elfmcys.ysm.geckolib3.core.controller.transition.IBlendTransition;
import com.elfmcys.ysm.geckolib3.core.controller.transition.LinearBlendTransition;
import com.elfmcys.ysm.geckolib3.core.controller.transition.SegmentedBlendTransition;
import com.elfmcys.ysm.geckolib3.core.keyframe.BoneAnimation;
import com.elfmcys.ysm.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.elfmcys.ysm.geckolib3.core.keyframe.bone.BoneKeyFrameProcessor;
import com.elfmcys.ysm.geckolib3.core.keyframe.bone.EasingType;
import com.elfmcys.ysm.geckolib3.core.keyframe.bone.RawBoneKeyFrame;
import com.elfmcys.ysm.geckolib3.core.keyframe.event.EventKeyFrame;
import com.elfmcys.ysm.geckolib3.core.molang.MolangParser;
import com.elfmcys.ysm.geckolib3.core.molang.value.IValue;
import com.elfmcys.ysm.geckolib3.file.AnimationControllerFile;
import mixel.asset.model.data.AnimationControllerOuterClass;
import mixel.asset.model.data.AnimationOuterClass;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import us.hebi.quickbuf.RepeatedMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AnimationProtoMapper {
    private AnimationProtoMapper() {
    }

    public static Animation animation(AnimationOuterClass.Animation source) {
        var parser = CustomMolangParser.rentInstance();
        try {
            return animation(source, parser);
        } finally {
            CustomMolangParser.returnInstance(parser);
        }
    }

    public static AnimationControllerFile controllerFile(AnimationControllerOuterClass.AnimationControllerFile source) {
        var parser = CustomMolangParser.rentInstance();
        try {
            var controllers = new Object2ReferenceOpenHashMap<String, AnimationControllerData>();
            var sourceControllers = source.hasControllers()
                    ? source.getControllers() : List.<AnimationControllerOuterClass.AnimationController>of();
            for (var controller : sourceControllers) {
                var states = new AnimationControllerState[
                        controller.hasStates() ? controller.getStates().length() : 0];
                for (var index = 0; index < states.length; index++) {
                    states[index] = state(controller.getStates().get(index), parser);
                }
                controllers.put(controller.getName(), new AnimationControllerData(controller.getDefaultState(), states));
            }
            return new AnimationControllerFile(controllers);
        } finally {
            CustomMolangParser.returnInstance(parser);
        }
    }

    private static Animation animation(AnimationOuterClass.Animation source, MolangParser parser) {
        var sourceBones = source.hasBoneAnimations()
                ? source.getBoneAnimations() : null;
        var bones = new ReferenceArrayList<BoneAnimation>(
                sourceBones != null ? sourceBones.length() : 0);
        if (sourceBones != null) {
            for (var sourceBone : sourceBones) {
                bones.add(new BoneAnimation(sourceBone.getBoneName(),
                        frames(sourceBone.hasRotation() ? sourceBone.getRotation() : List.of(), parser, true),
                        frames(sourceBone.hasPosition() ? sourceBone.getPosition() : List.of(), parser, false),
                        frames(sourceBone.hasScale() ? sourceBone.getScale() : List.of(), parser, false)));
            }
        }

        var sourceInstructions = source.hasInstructionKeyframes()
                ? source.getInstructionKeyframes() : null;
        var instructions = new ReferenceArrayList<EventKeyFrame<IValue[]>>(
                sourceInstructions != null ? sourceInstructions.length() : 0);
        if (sourceInstructions != null) {
            for (var sourceFrame : sourceInstructions) {
                var values = new IValue[sourceFrame.hasExpr() ? sourceFrame.getExpr().length() : 0];
                for (var index = 0; index < values.length; index++) {
                    values[index] = parser.parseExpression(sourceFrame.getExpr().get(index), false);
                }
                instructions.add(new EventKeyFrame<>(sourceFrame.getStartTick(), values));
            }
        }
        instructions.sort(Comparator.comparingDouble(EventKeyFrame::getStartTick));

        var blendWeight = source.hasBlendWeight() ? value(source.getBlendWeight(), parser) : null;
        return new Animation(source.getName(), source.getLength(), loop(source.getLoop()), blendWeight,
                bones, new ReferenceArrayList<>(), instructions);
    }

    private static List<BoneKeyFrame> frames(
            Iterable<AnimationOuterClass.BoneKeyFrame> source, MolangParser parser, boolean rotation) {
        var raw = new ReferenceArrayList<RawBoneKeyFrame>();
        for (var frame : source) {
            var target = new RawBoneKeyFrame();
            target.startTick = frame.getStartTick();
            target.easingType = frame.getEasingType() == AnimationOuterClass.EasingType.EASING_TYPE_CATMULLROM
                    ? EasingType.CATMULLROM : EasingType.LINEAR;
            if (frame.hasPre()) {
                assign(frame.getPre(), parser, target, false);
            }
            target.contiguous = !frame.hasPost() || frame.getPost().length() == 0;
            if (!target.contiguous) {
                assign(frame.getPost(), parser, target, true);
            }
            raw.add(target);
        }
        raw.sort(Comparator.comparingDouble(RawBoneKeyFrame::startTick));
        return BoneKeyFrameProcessor.process(raw, rotation);
    }

    private static void assign(RepeatedMessage<AnimationOuterClass.MolangValue> source,
                               MolangParser parser, RawBoneKeyFrame target, boolean post) {
        if (source.length() == 0) {
            return;
        }
        var x = value(source.get(0), parser);
        var y = source.length() >= 3 ? value(source.get(1), parser) : x;
        var z = source.length() >= 3 ? value(source.get(2), parser) : x;
        if (post) {
            target.postXValue = x;
            target.postYValue = y;
            target.postZValue = z;
        } else {
            target.preXValue = x;
            target.preYValue = y;
            target.preZValue = z;
        }
    }

    private static IValue value(AnimationOuterClass.MolangValue source, MolangParser parser) {
        if (source.hasNum()) {
            return parser.getConstant(source.getNum());
        }
        if (source.hasStr()) {
            return parser.parseExpression(source.getStr(), false);
        }
        return parser.getConstant(0);
    }

    @SuppressWarnings("unchecked")
    private static AnimationControllerState state(AnimationControllerOuterClass.State source, MolangParser parser) {
        var animations = new Pair[source.hasAnimations() ? source.getAnimations().length() : 0];
        for (var index = 0; index < animations.length; index++) {
            var animation = source.getAnimations().get(index);
            animations[index] = Pair.of(animation.getKey(), StringUtils.isBlank(animation.getValue())
                    ? null : parser.parseExpression(animation.getValue(), false));
        }
        var transitions = new Pair[source.hasTransitions() ? source.getTransitions().length() : 0];
        for (var index = 0; index < transitions.length; index++) {
            var transition = source.getTransitions().get(index);
            transitions[index] = Pair.of(transition.getDst(), parser.parseExpression(transition.getCondition(), false));
        }
        var onEntry = new IValue[source.hasOnEntry() ? source.getOnEntry().length() : 0];
        for (var index = 0; index < onEntry.length; index++) {
            onEntry[index] = parser.parseExpression(source.getOnEntry().get(index), false);
        }
        var onExit = new IValue[source.hasOnExit() ? source.getOnExit().length() : 0];
        for (var index = 0; index < onExit.length; index++) {
            onExit[index] = parser.parseExpression(source.getOnExit().get(index), false);
        }
        return new AnimationControllerState(source.getName(), animations, transitions, new String[0],
                onEntry, onExit, blend(source), source.getBlendViaShortestPath());
    }

    private static IBlendTransition blend(AnimationControllerOuterClass.State source) {
        if (!source.hasBlendTransition()) {
            return new LinearBlendTransition(0);
        }
        var blend = source.getBlendTransition();
        if (blend.hasLinearLength()) {
            return new LinearBlendTransition(blend.getLinearLength());
        }
        if (!blend.hasPoints() || blend.getPoints().length() < 2) {
            return new LinearBlendTransition(0);
        }
        var points = new ArrayList<AnimationControllerOuterClass.BlendPoint>();
        blend.getPoints().forEach(points::add);
        points.sort(Comparator.comparingDouble(AnimationControllerOuterClass.BlendPoint::getKey));
        var times = new float[points.size()];
        var positions = new float[points.size()];
        for (var index = 0; index < points.size(); index++) {
            times[index] = points.get(index).getKey();
            positions[index] = points.get(index).getValue();
        }
        return new SegmentedBlendTransition(times, positions);
    }

    private static LoopType loop(AnimationOuterClass.LoopType loop) {
        return switch (loop) {
            case LOOP_TYPE_LOOP -> LoopType.LOOP;
            case LOOP_TYPE_HOLD_ON_LAST_FRAME -> LoopType.HOLD_ON_LAST_FRAME;
            default -> LoopType.PLAY_ONCE;
        };
    }
}
