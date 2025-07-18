package com.elfmcys.yesstevemodel.geckolib3.core.util;

import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class MathUtil {
    private static final float DEGREES_TO_RADIANS = Mth.DEG_TO_RAD;
    private static final float RADIANS_TO_DEGREES = Mth.RAD_TO_DEG;
    public static final float PI = (float) Math.PI;

    public static final float ROUND = (float) Math.toRadians(360f);
    public static final float HALF_ROUND = (float) Math.toRadians(180f);
    public static final float RIGHT_ANG = (float) Math.toRadians(90f);

    public static final Vector3f ZERO = new Vector3f(0.0f, 0.0f, 0.0f);
    public static final Vector3f ONE = new Vector3f(1.0f, 1.0f, 1.0f);

    public static void lerpRotationValues(float percentCompleted, Vector3f begin, Vector3f end, Vector3f dst) {
        var temp = new Vector3f(end).sub(begin);
        MathUtil.wrapRotation(temp, temp);
        end.sub(temp, temp);
        MathUtil.lerpValues(percentCompleted, temp, end, dst);
    }

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

    public static Vector3f normalizeRotation(Vector3f value, Vector3f initRot, float weight) {
        var dst = new Vector3f();
        normalizeRotation(value, initRot, weight, dst);
        return dst;
    }

    public static void normalizeRotation(Vector3f value, Vector3f initRot, float weight, Vector3f dst) {
        var temp = wrapRotation(value)
                .mul(weight).add(initRot);

        if ((temp.y < RIGHT_ANG && temp.y > -RIGHT_ANG)
                || (temp.z <= RIGHT_ANG && temp.z >= -RIGHT_ANG)
                || (temp.x <= RIGHT_ANG && temp.x >= -RIGHT_ANG)) {
            wrapRotation(value, dst);
            return;
        }

        temp.y += temp.y > RIGHT_ANG ? -RIGHT_ANG : RIGHT_ANG;
        temp.x += temp.x >= 0 ? -HALF_ROUND : HALF_ROUND;
        temp.z += temp.z >= 0 ? -HALF_ROUND : HALF_ROUND;

        wrapRotation(temp.sub(initRot).div(weight), dst);
    }

    public static void wrapRotation(Vector3f value, Vector3f dst) {
        dst.set(wrapRotation(value.x), wrapRotation(value.y), wrapRotation(value.z));
    }

    public static Vector3f wrapRotation(Vector3f value) {
        return new Vector3f(wrapRotation(value.x), wrapRotation(value.y), wrapRotation(value.z));
    }

    public static float wrapRotation(float value) {
        float f = value % ROUND;
        if (f >= HALF_ROUND) {
            f -= ROUND;
        }

        if (f < -HALF_ROUND) {
            f += ROUND;
        }

        return f;
    }

    public static Vector3f computeWeightedScale(Vector3f value, float weight) {
        return new Vector3f(computeWeightedScale(value.x, weight), computeWeightedScale(value.y, weight), computeWeightedScale(value.z, weight));
    }
    public static void computeWeightedScale(Vector3f value, float weight, Vector3f dst) {
        dst.set(computeWeightedScale(value.x, weight), computeWeightedScale(value.y, weight), computeWeightedScale(value.z, weight));
    }

    public static float computeWeightedScale(float value, float weight) {
        return 1f + (value - 1f) * weight;
    }
}