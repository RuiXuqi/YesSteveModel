package com.elfmcys.yesstevemodel.geckolib3.core.util;

import org.joml.Vector3f;

public class MathUtil {
    private static final float DEGREES_TO_RADIANS = 0.017453292519943295F;

    public static Vector3f lerpValues(double percentCompleted, Vector3f begin, Vector3f end) {
        return new Vector3f(lerpValues(percentCompleted, begin.x(), end.x()),
                lerpValues(percentCompleted, begin.y(), end.y()),
                lerpValues(percentCompleted, begin.z(), end.z()));
    }

    public static float lerpValues(double percentCompleted, double startValue, double endValue) {
        return (float) (startValue + percentCompleted * (endValue - startValue));
    }

    public static Vector3f catmullRom(double percentCompleted, Vector3f left, Vector3f begin, Vector3f end, Vector3f right) {
        return new Vector3f(catmullRom(percentCompleted, left.x(), begin.x(), end.x(), right.x()),
                catmullRom(percentCompleted, left.y(), begin.y(), end.y(), right.y()),
                catmullRom(percentCompleted, left.z(), begin.z(), end.z(), right.z()));
    }

    public static float catmullRom(double percent, double left, double begin, double end, double right) {
        double v0 = (end - left) * 0.5;
        double v1 = (right - begin) * 0.5;
        double t2 = percent * percent;
        double t3 = percent * t2;
        return (float) ((2 * begin - 2 * end + v0 + v1) * t3 + (-3 * begin + 3 * end - 2 * v0 - v1) * t2 + v0 * percent + begin);
    }

    public static Vector3f rotLerp(float percent, Vector3f start, Vector3f end) {
        return new Vector3f(MathUtil.rotLerp(percent, start.x, end.x),
                MathUtil.rotLerp(percent, start.y, end.y),
                MathUtil.rotLerp(percent, start.z, end.z));
    }

    public static float rotLerp(float percent, float start, float end) {
        return end - (1 - percent) * wrapDegrees(end - start);
    }

    public static float wrapDegrees(float value) {
        float f = value % (360.0F * DEGREES_TO_RADIANS);
        if (f >= (180.0F * DEGREES_TO_RADIANS)) {
            f -= (360.0F * DEGREES_TO_RADIANS);
        }

        if (f < (-180.0F * DEGREES_TO_RADIANS)) {
            f += (360.0F * DEGREES_TO_RADIANS);
        }

        return f;
    }
}