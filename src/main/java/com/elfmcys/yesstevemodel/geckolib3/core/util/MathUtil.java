package com.elfmcys.yesstevemodel.geckolib3.core.util;

import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class MathUtil {
    private static final float DEGREES_TO_RADIANS = Mth.DEG_TO_RAD;
    private static final float RADIANS_TO_DEGREES = Mth.RAD_TO_DEG;
    public static final float PI = (float) Math.PI;

    public static Vector3f lerpValues(float percentCompleted, Vector3f begin, Vector3f end) {
        return new Vector3f(lerpValues(percentCompleted, begin.x(), end.x()),
                lerpValues(percentCompleted, begin.y(), end.y()),
                lerpValues(percentCompleted, begin.z(), end.z()));
    }

    public static void lerpValues(float percentCompleted, Vector3f begin, Vector3f end, Vector3f dst) {
        dst.set(lerpValues(percentCompleted, begin.x(), end.x()),
                lerpValues(percentCompleted, begin.y(), end.y()),
                lerpValues(percentCompleted, begin.z(), end.z()));
    }

    public static float lerpValues(float percentCompleted, float startValue, float endValue) {
        return (startValue + percentCompleted * (endValue - startValue));
    }

    public static Vector3f catmullRom(float percentCompleted, Vector3f left, Vector3f begin, Vector3f end, Vector3f right) {
        return new Vector3f(catmullRom(percentCompleted, left.x(), begin.x(), end.x(), right.x()),
                catmullRom(percentCompleted, left.y(), begin.y(), end.y(), right.y()),
                catmullRom(percentCompleted, left.z(), begin.z(), end.z(), right.z()));
    }

    public static float catmullRom(float percent, float left, float begin, float end, float right) {
        float v0 = (end - left) * 0.5f;
        float v1 = (right - begin) * 0.5f;
        float t2 = percent * percent;
        float t3 = percent * t2;
        return ((2 * begin - 2 * end + v0 + v1) * t3 + (-3 * begin + 3 * end - 2 * v0 - v1) * t2 + v0 * percent + begin);
    }

    public static float degreesToRadians(float degrees) {
        return degrees * DEGREES_TO_RADIANS;
    }

    public static float radiansToDegrees(float degrees) {
        return degrees * RADIANS_TO_DEGREES;
    }

    public static Vector3f wrapRadians(Vector3f value) {
        return new Vector3f(wrapRadians(value.x), wrapRadians(value.y), wrapRadians(value.z));
    }

    public static float wrapRadians(float value) {
        float f = value % (360.0F * DEGREES_TO_RADIANS);
        if (f >= (180.0F * DEGREES_TO_RADIANS)) {
            f -= (360.0F * DEGREES_TO_RADIANS);
        }

        if (f < (-180.0F * DEGREES_TO_RADIANS)) {
            f += (360.0F * DEGREES_TO_RADIANS);
        }

        return f;
    }

    public static Vector3f computeWeightedScale(Vector3f value, float weight) {
        return new Vector3f(computeWeightedScale(value.x, weight), computeWeightedScale(value.y, weight), computeWeightedScale(value.z, weight));
    }

    public static float computeWeightedScale(float value, float weight) {
        return 1f + (value - 1f) * weight;
    }
}