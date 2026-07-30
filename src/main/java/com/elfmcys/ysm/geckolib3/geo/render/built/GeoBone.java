package com.elfmcys.ysm.geckolib3.geo.render.built;

import com.elfmcys.ysm.geckolib3.core.molang.util.StringPool;

// Native Access
public class GeoBone {
    private final String name;
    private final int pooledName;

    private final boolean isHidden;
    private final boolean areCubesHidden;
    private final boolean areChildrenHidden;

    private final float pivotX;
    private final float pivotY;
    private final float pivotZ;
    private final float rotationX;
    private final float rotationY;
    private final float rotationZ;

    // Native Access
    @SuppressWarnings("unused")
    public GeoBone(String name, boolean isHidden, boolean areCubesHidden, boolean hideChildBonesToo, float rotationPointX, float rotationPointY, float rotationPointZ, float rotateX, float rotateY, float rotateZ) {
        this.name = name;
        this.pooledName = StringPool.computeIfAbsent(name);

        this.isHidden = isHidden;
        this.areCubesHidden = areCubesHidden;
        this.areChildrenHidden = hideChildBonesToo;

        this.pivotX = rotationPointX;
        this.pivotY = rotationPointY;
        this.pivotZ = rotationPointZ;

        this.rotationX = rotateX;
        this.rotationY = rotateY;
        this.rotationZ = rotateZ;
    }

    public String name() {
        return name;
    }

    public int pooledName() {
        return pooledName;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public boolean areCubesHidden() {
        return areCubesHidden;
    }

    public boolean areChildrenHidden() {
        return areChildrenHidden;
    }

    public float pivotX() {
        return pivotX;
    }

    public float pivotY() {
        return pivotY;
    }

    public float pivotZ() {
        return pivotZ;
    }

    public float rotationX() {
        return rotationX;
    }

    public float rotationY() {
        return rotationY;
    }

    public float rotationZ() {
        return rotationZ;
    }
}
