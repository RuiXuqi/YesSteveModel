package com.elfmcys.ysm.geckolib3.model;

import com.elfmcys.ysm.geckolib3.core.processor.BoneView;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoBone;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class AnimatedGeoBone implements BoneView {
    private static final int ROTATION_OFFSET = 0;
    private static final int POSITION_OFFSET = 3;
    private static final int SCALE_OFFSET = 6;
    private static final int CUBES_HIDDEN_OFFSET = 9;
    private static final int CHILDREN_HIDDEN_OFFSET = 10;
    private static final int LOCATOR_SEQUENCE_OFFSET = 11;

    private final GeoBone bone;
    private final float[] attributes;
    private final int baseOffset;

    public AnimatedGeoBone(GeoBone bone, float[] attributes, int boneIndex) {
        this.bone = bone;
        this.attributes = attributes;
        this.baseOffset = boneIndex * AnimatedGeoModel.BONE_ATTRIBUTE_COUNT;

        var rotation = bone.rotation();
        setRotation(rotation.x, rotation.y, rotation.z);
        setPosition(0, 0, 0);
        setScale(1, 1, 1);
        var locator = bone.locatorType();
        attributes[baseOffset + LOCATOR_SEQUENCE_OFFSET] =
                locator == null ? 0 : Byte.toUnsignedInt(locator.seq());
    }

    public GeoBone getBoneData() {
        return bone;
    }

    public Vector3f getInitialRotation() {
        return bone.rotation();
    }

    public Vector3f getPivot() {
        return bone.pivot();
    }

    @Override
    public String getName() {
        return bone.name();
    }

    @Override
    public int getPooledName() {
        return bone.pooledName();
    }

    @Override
    public float getPositionX() {
        return attributes[baseOffset + POSITION_OFFSET];
    }

    @Override
    public float getPositionY() {
        return attributes[baseOffset + POSITION_OFFSET + 1];
    }

    @Override
    public float getPositionZ() {
        return attributes[baseOffset + POSITION_OFFSET + 2];
    }

    public void setPosition(float x, float y, float z) {
        attributes[baseOffset + POSITION_OFFSET] = x;
        attributes[baseOffset + POSITION_OFFSET + 1] = y;
        attributes[baseOffset + POSITION_OFFSET + 2] = z;
    }

    public void setPosition(Vector3fc value) {
        setPosition(value.x(), value.y(), value.z());
    }

    @Override
    public float getScaleX() {
        return attributes[baseOffset + SCALE_OFFSET];
    }

    @Override
    public float getScaleY() {
        return attributes[baseOffset + SCALE_OFFSET + 1];
    }

    @Override
    public float getScaleZ() {
        return attributes[baseOffset + SCALE_OFFSET + 2];
    }

    public void setScale(float x, float y, float z) {
        attributes[baseOffset + SCALE_OFFSET] = x;
        attributes[baseOffset + SCALE_OFFSET + 1] = y;
        attributes[baseOffset + SCALE_OFFSET + 2] = z;
    }

    public void setScale(Vector3fc value) {
        setScale(value.x(), value.y(), value.z());
    }

    @Override
    public float getRotationX() {
        return attributes[baseOffset + ROTATION_OFFSET];
    }

    @Override
    public float getRotationY() {
        return attributes[baseOffset + ROTATION_OFFSET + 1];
    }

    @Override
    public float getRotationZ() {
        return attributes[baseOffset + ROTATION_OFFSET + 2];
    }

    public void setRotation(float x, float y, float z) {
        attributes[baseOffset + ROTATION_OFFSET] = x;
        attributes[baseOffset + ROTATION_OFFSET + 1] = y;
        attributes[baseOffset + ROTATION_OFFSET + 2] = z;
    }

    public void setRotationX(float x) {
        attributes[baseOffset + ROTATION_OFFSET] = x;
    }

    public void setRotationY(float y) {
        attributes[baseOffset + ROTATION_OFFSET + 1] = y;
    }

    public void setRotationZ(float z) {
        attributes[baseOffset + ROTATION_OFFSET + 2] = z;
    }

    public void setRotation(Vector3fc value) {
        setRotation(value.x(), value.y(), value.z());
    }

    @Override
    public float getInitialRotationX() {
        return bone.rotation().x;
    }

    @Override
    public float getInitialRotationY() {
        return bone.rotation().y;
    }

    @Override
    public float getInitialRotationZ() {
        return bone.rotation().z;
    }

    @Override
    public float getPivotX() {
        return bone.pivot().x;
    }

    @Override
    public float getPivotY() {
        return bone.pivot().y;
    }

    @Override
    public float getPivotZ() {
        return bone.pivot().z;
    }

    @Override
    public boolean areCubesHidden() {
        return attributes[baseOffset + CUBES_HIDDEN_OFFSET] != 0;
    }

    @Override
    public boolean areChildrenHidden() {
        return attributes[baseOffset + CHILDREN_HIDDEN_OFFSET] != 0;
    }

    public void setHidden(boolean hidden) {
        setHidden(hidden, hidden);
    }

    public void setHidden(boolean cubesHidden, boolean childrenHidden) {
        attributes[baseOffset + CUBES_HIDDEN_OFFSET] = cubesHidden ? 1 : 0;
        attributes[baseOffset + CHILDREN_HIDDEN_OFFSET] = childrenHidden ? 1 : 0;
    }

    // TODO
//    @SuppressWarnings("unchecked")
//    public <T> T getTlmBone() {
//        if (this.tlmBone == null) {
//            // FIXME: 有可能会触发类加载？
//            this.tlmBone = TlmConverterHelper.convertToTlmGeoBone(this);
//        }
//        return (T) this.tlmBone;
//    }
}
