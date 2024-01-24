package com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.DoubleValue;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.RotationValue;

// 该类为了缓解 jni 的性能问题，有很多奇怪的设计，但不影响正常运作，也不要尝试“优化”
// Native Access：所有字段都有写入，不要尝试添加 final
@SuppressWarnings("FieldMayBeFinal,unused")
public class RawBoneKeyFrame {
    private double startTick;
    private EasingType easingType;

    private double preX;
    private IValue preXValue;
    private double preY;
    private IValue preYValue;
    private double preZ;
    private IValue preZValue;

    private double postX;
    private IValue postXValue;
    private double postY;
    private IValue postYValue;
    private double postZ;
    private IValue postZValue;

    private boolean contiguous;

    private Vector3v preValue;
    private Vector3v postValue;

    // 该构造函数只是个占位符，实际上不会调用
    public RawBoneKeyFrame() {
    }

    private IValue getValue(IValue value, double primitive, boolean isRotation, boolean flip) {
        if (value == null) {
            if (isRotation) {
                return new DoubleValue(RotationValue.processValue(primitive, flip));
            } else {
                return new DoubleValue(primitive);
            }
        }
        if (isRotation) {
            return new RotationValue(value, flip);
        } else {
            return value;
        }
    }

    public void init(boolean isRotation) {
        if (preValue != null) {
            return;
        }

        preValue = new Vector3v(
                getValue(this.preXValue, this.preX, isRotation, true),
                getValue(this.preYValue, this.preY, isRotation, true),
                getValue(this.preZValue, this.preZ, isRotation, false));
        if (contiguous) {
            postValue = preValue;
        } else {
            postValue = new Vector3v(
                    getValue(this.postXValue, this.postX, isRotation, true),
                    getValue(this.postYValue, this.postY, isRotation, true),
                    getValue(this.postZValue, this.postZ, isRotation, false));
        }
    }

    public double startTick() {
        return startTick;
    }

    public EasingType easingType() {
        return easingType;
    }

    public Vector3v preValue() {
        return preValue;
    }

    public Vector3v postValue() {
        return postValue;
    }
}
