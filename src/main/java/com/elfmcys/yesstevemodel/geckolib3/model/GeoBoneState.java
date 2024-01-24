package com.elfmcys.yesstevemodel.geckolib3.model;

import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoBone;

public class GeoBoneState implements IBone {
    // Native Association: 所有 index 都有关联
    private static final int INDEX_ROTATION_X = 0;
    private static final int INDEX_ROTATION_Y = 1;
    private static final int INDEX_ROTATION_Z = 2;

    private static final int INDEX_POSITION_X = 3;
    private static final int INDEX_POSITION_Y = 4;
    private static final int INDEX_POSITION_Z = 5;

    private static final int INDEX_SCALE_X = 6;
    private static final int INDEX_SCALE_Y = 7;
    private static final int INDEX_SCALE_Z = 8;

    private static final int INDEX_IS_HIDDEN = 9;
    private static final int INDEX_IS_CHILDREN_HIDDEN = 10;
    private static final int INDEX_IS_GLOWING = 11;

    private final String name;
    private final float pivotX;
    private final float pivotY;
    private final float pivotZ;

    private final float[] state;
    private final int stateOffset;
    private final BoneSnapshot initialSnapshot;

    public GeoBoneState(GeoBone bone, float[] state, int stateOffset) {
        this.name = bone.name();
        this.pivotX = bone.pivotX();
        this.pivotY = bone.pivotY();
        this.pivotZ = bone.pivotZ();

        this.state = state;
        this.stateOffset = stateOffset;
        this.setHidden(bone.isHidden(), bone.areChildrenHidden());
        this.setRotationX(bone.rotationX());
        this.setRotationY(bone.rotationY());
        this.setRotationZ(bone.rotationZ());
        this.setScaleX(1);
        this.setScaleY(1);
        this.setScaleZ(1);
        this.initialSnapshot = new BoneTopLevelSnapshot(this);
    }

    @Override
    public BoneSnapshot getInitialSnapshot() {
        return this.initialSnapshot;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public float getRotationX() {
        return this.state[stateOffset + INDEX_ROTATION_X];
    }

    @Override
    public void setRotationX(float value) {
        this.state[stateOffset + INDEX_ROTATION_X] = value;
    }

    @Override
    public float getRotationY() {
        return this.state[stateOffset + INDEX_ROTATION_Y];
    }

    @Override
    public void setRotationY(float value) {
        this.state[stateOffset + INDEX_ROTATION_Y] = value;
    }

    @Override
    public float getRotationZ() {
        return this.state[stateOffset + INDEX_ROTATION_Z];
    }

    @Override
    public void setRotationZ(float value) {
        this.state[stateOffset + INDEX_ROTATION_Z] = value;
    }

    @Override
    public float getPositionX() {
        return this.state[stateOffset + INDEX_POSITION_X];
    }

    @Override
    public void setPositionX(float value) {
        this.state[stateOffset + INDEX_POSITION_X] = value;
    }

    @Override
    public float getPositionY() {
        return this.state[stateOffset + INDEX_POSITION_Y];
    }

    @Override
    public void setPositionY(float value) {
        this.state[stateOffset + INDEX_POSITION_Y] = value;
    }

    @Override
    public float getPositionZ() {
        return this.state[stateOffset + INDEX_POSITION_Z];
    }

    @Override
    public void setPositionZ(float value) {
        this.state[stateOffset + INDEX_POSITION_Z] = value;
    }

    @Override
    public float getScaleX() {
        return this.state[stateOffset + INDEX_SCALE_X];
    }

    @Override
    public void setScaleX(float value) {
        this.state[stateOffset + INDEX_SCALE_X] = value;
    }

    @Override
    public float getScaleY() {
        return this.state[stateOffset + INDEX_SCALE_Y];
    }

    @Override
    public void setScaleY(float value) {
        this.state[stateOffset + INDEX_SCALE_Y] = value;
    }

    @Override
    public float getScaleZ() {
        return this.state[stateOffset + INDEX_SCALE_Z];
    }

    @Override
    public void setScaleZ(float value) {
        this.state[stateOffset + INDEX_SCALE_Z] = value;
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
        return this.state[stateOffset + INDEX_IS_HIDDEN] == 1;
    }

    @Override
    public void setHidden(boolean hidden) {
        this.setHidden(hidden, hidden);
    }

    @Override
    public boolean areChildrenHidden() {
        return this.state[stateOffset + INDEX_IS_CHILDREN_HIDDEN] == 1;
    }

    @Override
    public void setHidden(boolean selfHidden, boolean skipChildRendering) {
        this.state[stateOffset + INDEX_IS_HIDDEN] = selfHidden ? 1 : 0;
        this.state[stateOffset + INDEX_IS_CHILDREN_HIDDEN] = skipChildRendering ? 1 : 0;
    }

    @Override
    public boolean isGlowing() {
        return this.state[stateOffset + INDEX_IS_GLOWING] == 1;
    }

    @Override
    public void setGlowing(boolean glowing) {
        this.state[stateOffset + INDEX_IS_GLOWING] = glowing ? 1 : 0;
    }
}
