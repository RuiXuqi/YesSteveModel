package com.elfmcys.ysm.geckolib3.model;

import com.elfmcys.ysm.client.compat.touhoulittlemaid.util.TlmConverterHelper;
import com.elfmcys.ysm.geckolib3.core.processor.IBone;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoBone;
import org.joml.Vector3f;

public class GeoBoneState implements IBone {
    // Native Association: 所有 index 都有关联
    private static final int IN_IDX_ROTATION_X = 0;
    private static final int IN_IDX_ROTATION_Y = 1;
    private static final int IN_IDX_ROTATION_Z = 2;

    private static final int IN_IDX_POSITION_X = 3;
    private static final int IN_IDX_POSITION_Y = 4;
    private static final int IN_IDX_POSITION_Z = 5;

    private static final int IN_IDX_SCALE_X = 6;
    private static final int IN_IDX_SCALE_Y = 7;
    private static final int IN_IDX_SCALE_Z = 8;

    private static final int IN_IDX_HIDDEN = 9;
    private static final int IN_IDX_CHILDREN_HIDDEN = 10;
    private static final int IN_IDX_TRACKING = 11;

    private static final int OUT_IDX_ABS_PIVOT_X = 0;
    private static final int OUT_IDX_ABS_PIVOT_Y = 1;
    private static final int OUT_IDX_ABS_PIVOT_Z = 2;

    private final String name;
    private final int pooledName;

    private final float pivotX;
    private final float pivotY;
    private final float pivotZ;

    private final float[] inputState;
    private final int inputStateOffset;

    private final float[] outputState;
    private final int outputStateOffset;

    private final Vector3f initialRotation;

    /**
     * 仅用于 TLM 定位组获取的的 AnimationModel
     * <p>
     * FIXME: 其实不应该这样耦合的
     */
    private Object tlmBone = null;

    public GeoBoneState(GeoBone bone, float[] inputState, int inputStateOffset, float[] outputState, int outputStateOffset) {
        this.name = bone.name();
        this.pooledName = bone.pooledName();

        this.pivotX = bone.pivotX();
        this.pivotY = bone.pivotY();
        this.pivotZ = bone.pivotZ();

        this.inputState = inputState;
        this.inputStateOffset = inputStateOffset;
        this.outputState = outputState;
        this.outputStateOffset = outputStateOffset;

        this.setHidden(bone.isHidden(), bone.areChildrenHidden());
        this.setRotationX(bone.rotationX());
        this.setRotationY(bone.rotationY());
        this.setRotationZ(bone.rotationZ());
        this.setScaleX(1);
        this.setScaleY(1);
        this.setScaleZ(1);

        this.initialRotation = new Vector3f(bone.rotationX(), bone.rotationY(), bone.rotationZ());
    }

    @Override
    public Vector3f getInitialRotation() {
        return this.initialRotation;
    }

    @Override
    public float getAbsolutePivotX() {
        return outputState[outputStateOffset + OUT_IDX_ABS_PIVOT_X];
    }

    @Override
    public float getAbsolutePivotY() {
        return outputState[outputStateOffset + OUT_IDX_ABS_PIVOT_Y];
    }

    @Override
    public float getAbsolutePivotZ() {
        return outputState[outputStateOffset + OUT_IDX_ABS_PIVOT_Z];
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getPooledName() {
        return this.pooledName;
    }

    @Override
    public float getRotationX() {
        return this.inputState[inputStateOffset + IN_IDX_ROTATION_X];
    }

    @Override
    public void setRotationX(float value) {
        this.inputState[inputStateOffset + IN_IDX_ROTATION_X] = value;
    }

    @Override
    public float getRotationY() {
        return this.inputState[inputStateOffset + IN_IDX_ROTATION_Y];
    }

    @Override
    public void setRotationY(float value) {
        this.inputState[inputStateOffset + IN_IDX_ROTATION_Y] = value;
    }

    @Override
    public float getRotationZ() {
        return this.inputState[inputStateOffset + IN_IDX_ROTATION_Z];
    }

    @Override
    public void setRotationZ(float value) {
        this.inputState[inputStateOffset + IN_IDX_ROTATION_Z] = value;
    }

    @Override
    public float getPositionX() {
        return this.inputState[inputStateOffset + IN_IDX_POSITION_X];
    }

    @Override
    public void setPositionX(float value) {
        this.inputState[inputStateOffset + IN_IDX_POSITION_X] = value;
    }

    @Override
    public float getPositionY() {
        return this.inputState[inputStateOffset + IN_IDX_POSITION_Y];
    }

    @Override
    public void setPositionY(float value) {
        this.inputState[inputStateOffset + IN_IDX_POSITION_Y] = value;
    }

    @Override
    public float getPositionZ() {
        return this.inputState[inputStateOffset + IN_IDX_POSITION_Z];
    }

    @Override
    public void setPositionZ(float value) {
        this.inputState[inputStateOffset + IN_IDX_POSITION_Z] = value;
    }

    @Override
    public float getScaleX() {
        return this.inputState[inputStateOffset + IN_IDX_SCALE_X];
    }

    @Override
    public void setScaleX(float value) {
        this.inputState[inputStateOffset + IN_IDX_SCALE_X] = value;
    }

    @Override
    public float getScaleY() {
        return this.inputState[inputStateOffset + IN_IDX_SCALE_Y];
    }

    @Override
    public void setScaleY(float value) {
        this.inputState[inputStateOffset + IN_IDX_SCALE_Y] = value;
    }

    @Override
    public float getScaleZ() {
        return this.inputState[inputStateOffset + IN_IDX_SCALE_Z];
    }

    @Override
    public void setScaleZ(float value) {
        this.inputState[inputStateOffset + IN_IDX_SCALE_Z] = value;
    }

    @Override
    public float getPivotX() {
        return this.pivotX;
    }

    @Override
    public float getPivotY() {
        return this.pivotY;
    }

    @Override
    public float getPivotZ() {
        return this.pivotZ;
    }

    @Override
    public boolean isHidden() {
        return this.inputState[inputStateOffset + IN_IDX_HIDDEN] == 1;
    }

    @Override
    public void setHidden(boolean hidden) {
        this.setHidden(hidden, hidden);
    }

    @Override
    public boolean areChildrenHidden() {
        return this.inputState[inputStateOffset + IN_IDX_CHILDREN_HIDDEN] == 1;
    }

    @Override
    public void setHidden(boolean selfHidden, boolean skipChildRendering) {
        this.inputState[inputStateOffset + IN_IDX_HIDDEN] = selfHidden ? 1 : 0;
        this.inputState[inputStateOffset + IN_IDX_CHILDREN_HIDDEN] = skipChildRendering ? 1 : 0;
    }

    @Override
    public boolean isTracking() {
        return this.inputState[inputStateOffset + IN_IDX_TRACKING] == 1;
    }

    @Override
    public void setTracking(boolean value) {
        this.inputState[inputStateOffset + IN_IDX_TRACKING] = value ? 1 : 0;
    }

    @SuppressWarnings("unchecked")
    public <T> T getTlmBone() {
        if (this.tlmBone == null) {
            // FIXME: 有可能会触发类加载？
            this.tlmBone = TlmConverterHelper.convertToTlmGeoBone(this);
        }
        return (T) this.tlmBone;
    }
}
