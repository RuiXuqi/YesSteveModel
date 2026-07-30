package com.elfmcys.ysm.geckolib3.core.controller;

import com.elfmcys.ysm.geckolib3.core.keyframe.AnimationVec3;
import com.elfmcys.ysm.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.ysm.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.ysm.molang.runtime.ExpressionEvaluator;

import java.util.Optional;

public interface IBoneAnimationQueue {
    BoneTopLevelSnapshot getSnapshot();

    Optional<AnimationVec3> pollRotationPoint(ExpressionEvaluator<MolangContext<?>> evaluator);

    Optional<AnimationVec3> pollPositionPoint(ExpressionEvaluator<MolangContext<?>> evaluator);

    Optional<AnimationVec3> pollScalePoint(ExpressionEvaluator<MolangContext<?>> evaluator);
}
