package com.elfmcys.yesstevemodel.geckolib3.geo.render.built;

import com.elfmcys.yesstevemodel.geckolib3.geo.raw.pojo.ModelProperties;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.util.CleanerUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

// Native Access
// 模型对象一定不能用 java 代码构建，否则无法渲染
public class GeoModel {
    @Nonnull
    public final List<GeoBone> sortedBones;

    @Nonnull
    public final List<GeoBone> leftHandBones;
    @Nonnull
    public final List<GeoBone> rightHandBones;
    @Nonnull
    public final List<GeoBone> elytraBones;
    @Nonnull
    public final List<GeoBone> tacPistolBones;
    @Nonnull
    public final List<GeoBone> tacRifleBones;
    @Nullable
    public final GeoBone firstPersonHead;
    @Nullable
    public final GeoBone firstPersonViewLocator;
    @Nonnull
    public final ModelProperties properties;

    public final boolean hasFirstPersonLeftArm;
    public final boolean hasFirstPersonRightArm;
    public final boolean hasfirstPersonBackground;

    @Nonnull
    public final float[] initialState;

    // Native Access
    @SuppressWarnings("all")
    private long nativeId;

    // Native Access
    public GeoModel(GeoBone[] sortedBones, GeoBone[] leftHandBones, GeoBone[] rightHandBones, GeoBone[] elytraBones, GeoBone[] tacPistolBones, GeoBone[] tacRifleBones, boolean hasFirstPersonLeftArm, boolean hasFirstPersonRightArm, boolean hasfirstPersonBackground, @Nullable GeoBone firstPersonHead, @Nullable GeoBone firstPersonViewLocator, ModelProperties properties) {
        this.sortedBones = ObjectArrayList.wrap(sortedBones);

        this.leftHandBones = ObjectArrayList.wrap(leftHandBones);
        this.rightHandBones = ObjectArrayList.wrap(rightHandBones);
        this.elytraBones = ObjectArrayList.wrap(elytraBones);
        this.tacPistolBones = ObjectArrayList.wrap(tacPistolBones);
        this.tacRifleBones = ObjectArrayList.wrap(tacRifleBones);
        this.hasFirstPersonLeftArm = hasFirstPersonLeftArm;
        this.hasFirstPersonRightArm = hasFirstPersonRightArm;
        this.hasfirstPersonBackground = hasfirstPersonBackground;
        this.firstPersonHead = firstPersonHead;
        this.firstPersonViewLocator = firstPersonViewLocator;
        this.properties = properties;

        this.initialState = new GeoModelState(this).state();
        CleanerUtil.ref(this, GeoModel::free);
    }

    @Nonnull
    public List<GeoBone> getSortedBones() {
        return sortedBones;
    }

    @Nonnull
    public float[] getInitialState() { return initialState; }

    private native void free();
}
