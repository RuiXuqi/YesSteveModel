package com.elfmcys.ysm.format.parser;

import com.elfmcys.ysm.format.parser.pojo.animation.Animation;
import com.elfmcys.ysm.format.parser.pojo.animation.AnimationFile;
import com.elfmcys.ysm.format.parser.pojo.animation.BoneAnimation;
import com.elfmcys.ysm.format.parser.pojo.animation.keyframe.BoneKeyFrame;
import com.elfmcys.ysm.format.parser.pojo.animation.keyframe.BoneKeyFrameList;
import com.elfmcys.ysm.format.parser.pojo.animation.value.UnionValue;
import com.elfmcys.ysm.geckolib3.core.builder.LoopType;
import com.elfmcys.ysm.geckolib3.core.keyframe.bone.EasingType;
import mixel.asset.model.data.AnimationOuterClass;

import java.util.Objects;
import java.util.function.Consumer;

public class AnimationBuilder {
    private AnimationBuilder() {
    }

    public static AnimationOuterClass.AnimationFile build(AnimationFile pojo) {
        var result = AnimationOuterClass.AnimationFile.newInstance();
        for (var animation : pojo.animations) {
            result.addAnimations(toProto(animation));
        }
        return result;
    }

    private static AnimationOuterClass.Animation toProto(Animation src) {
        var dst = AnimationOuterClass.Animation.newInstance()
                .setName(Objects.requireNonNullElse(src.animationName, ""))
                .setLength(src.animationLength)
                .setLoop(toProto(src.loop));
        if (src.blendWeight != null) {
            dst.setBlendWeight(toProto(src.blendWeight));
        }
        for (var boneAnimation : src.boneAnimations) {
            dst.addBoneAnimations(toProto(boneAnimation));
        }
        for (var instruction : src.customInstructionKeyframes) {
            var frame = AnimationOuterClass.InstructionKeyFrame.newInstance()
                    .setStartTick(instruction.startTick);
            for (var expr : instruction.eventData) {
                frame.addExpr(expr);
            }
            dst.addInstructionKeyframes(frame);
        }
        return dst;
    }

    private static AnimationOuterClass.BoneAnimation toProto(BoneAnimation src) {
        var dst = AnimationOuterClass.BoneAnimation.newInstance()
                .setBoneName(Objects.requireNonNullElse(src.boneName, ""));
        addKeyFrames(dst::addRotation, src.rotationKeyFrames);
        addKeyFrames(dst::addPosition, src.positionKeyFrames);
        addKeyFrames(dst::addScale, src.scaleKeyFrames);
        return dst;
    }

    private static void addKeyFrames(Consumer<AnimationOuterClass.BoneKeyFrame> adder, BoneKeyFrameList list) {
        for (var keyFrame : list.keyFrames) {
            adder.accept(toProto(keyFrame));
        }
    }

    private static AnimationOuterClass.BoneKeyFrame toProto(BoneKeyFrame src) {
        var dst = AnimationOuterClass.BoneKeyFrame.newInstance()
                .setStartTick(src.startTick)
                .setEasingType(src.easingType == EasingType.CATMULLROM
                        ? AnimationOuterClass.EasingType.EASING_TYPE_CATMULLROM
                        : AnimationOuterClass.EasingType.EASING_TYPE_LINEAR);
        if (src.preValue != null) {
            src.preValue.components.forEach(value -> dst.addPre(toProto(value)));
        }
        if (src.postValue != null) {
            src.postValue.components.forEach(value -> dst.addPost(toProto(value)));
        }
        return dst;
    }

    private static AnimationOuterClass.MolangValue toProto(UnionValue src) {
        var dst = AnimationOuterClass.MolangValue.newInstance();
        if (src.type == UnionValue.ValueType.FLOAT) {
            dst.setNum(src.floatValue);
        } else if (src.type == UnionValue.ValueType.STRING) {
            dst.setStr(src.stringValue);
        }
        return dst;
    }

    private static AnimationOuterClass.LoopType toProto(LoopType loop) {
        if (loop == LoopType.LOOP) {
            return AnimationOuterClass.LoopType.LOOP_TYPE_LOOP;
        }
        if (loop == LoopType.HOLD_ON_LAST_FRAME) {
            return AnimationOuterClass.LoopType.LOOP_TYPE_HOLD_ON_LAST_FRAME;
        }
        return AnimationOuterClass.LoopType.LOOP_TYPE_PLAY_ONCE;
    }
}
