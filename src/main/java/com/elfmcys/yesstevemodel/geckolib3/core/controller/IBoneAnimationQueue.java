package com.elfmcys.yesstevemodel.geckolib3.core.controller;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationMolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

import java.util.Optional;

public interface IBoneAnimationQueue {
    BoneTopLevelSnapshot getSnapshot();

    Optional<Vector3f> pollRotationPoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator);

    Optional<Vector3f> pollPositionPoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator);

    Optional<Vector3f> pollScalePoint(ExpressionEvaluator<AnimationMolangContext<?>> evaluator);
}
