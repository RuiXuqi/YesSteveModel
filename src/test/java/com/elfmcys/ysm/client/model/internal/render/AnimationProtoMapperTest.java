package com.elfmcys.ysm.client.model.internal.render;

import mixel.asset.model.data.AnimationOuterClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AnimationProtoMapperTest {
    @Test
    void treatsAbsentPostValuesAsAContiguousKeyframe() {
        var frame = AnimationOuterClass.BoneKeyFrame.newInstance()
                .setStartTick(0)
                .setEasingType(AnimationOuterClass.EasingType.EASING_TYPE_LINEAR)
                .addPre(AnimationOuterClass.MolangValue.newInstance().setNum(1));
        var bone = AnimationOuterClass.BoneAnimation.newInstance()
                .setBoneName("root")
                .addPosition(frame);
        var animation = AnimationOuterClass.Animation.newInstance()
                .setName("test")
                .setLength(0)
                .setLoop(AnimationOuterClass.LoopType.LOOP_TYPE_PLAY_ONCE)
                .addBoneAnimations(bone);

        assertDoesNotThrow(() -> AnimationProtoMapper.animation(animation));
    }
}
