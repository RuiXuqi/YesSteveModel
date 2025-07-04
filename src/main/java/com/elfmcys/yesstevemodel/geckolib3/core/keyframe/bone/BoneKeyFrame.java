package com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone;

import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.event.PointType;
import com.elfmcys.yesstevemodel.geckolib3.core.util.MathUtil;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public abstract class BoneKeyFrame {
    protected final float startTick;
    protected final float totalTick;
    protected final float endTick;
    protected final Vector3v beginPoint;

    public BoneKeyFrame(float startTick, float totalTick, Vector3v beginPoint) {
        this.startTick = startTick;
        this.totalTick = totalTick;
        this.endTick = startTick + totalTick;
        this.beginPoint = beginPoint;
    }

    public float getStartTick() {
        return startTick;
    }

    public float getTotalTick() {
        return totalTick;
    }

    public float getEndTick() {
        return endTick;
    }

    public abstract Vector3f getLerpPoint(ExpressionEvaluator<?> evaluator, float percentCompleted);

    public Vector3f getTransitionPoint(ExpressionEvaluator<?> evaluator, Vector3f offsetPoint, float percentCompleted) {
        if (isBegin(percentCompleted)) {
            return offsetPoint;
        }
        var dst = this.beginPoint.eval(evaluator);
        if (isEnd(percentCompleted)) {
            return dst;
        }
        return MathUtil.lerpValues(percentCompleted, offsetPoint, dst);
    }

    protected static boolean isBegin(float percentCompleted) {
        return percentCompleted < 0.00001;
    }

    protected static boolean isEnd(float percentCompleted) {
        return percentCompleted > 0.99999f;
    }
}
