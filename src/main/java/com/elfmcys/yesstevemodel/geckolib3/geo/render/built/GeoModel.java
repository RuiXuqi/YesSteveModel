package com.elfmcys.yesstevemodel.geckolib3.geo.render.built;

import com.elfmcys.yesstevemodel.geckolib3.geo.raw.pojo.GeoModelProperties;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.util.CleanerUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// Native Access
// 模型对象一定不能用 java 代码构建，否则无法渲染
public class GeoModel {
    @NotNull
    public final List<GeoBone> sortedBones;

    @NotNull
    public final List<String> leftHandBones;
    @NotNull
    public final List<String> rightHandBones;
    @NotNull
    public final List<String> elytraBones;
    @NotNull
    public final List<String> tacPistolBones;
    @NotNull
    public final List<String> tacRifleBones;
    @NotNull
    public final List<String> leftWaistBones;
    @NotNull
    public final List<String> rightWaistBones;
    @NotNull
    public final List<String> leftShoulderBones;
    @NotNull
    public final List<String> rightShoulderBones;

    @NotNull
    public final List<String> bladeBones;
    @NotNull
    public final List<String> sheathBones;

    @NotNull
    public final List<String> headBones;
    @NotNull
    public final List<String> backpackBones;

    public final boolean hasFirstPersonLeftArm;
    public final boolean hasFirstPersonRightArm;
    public final boolean hasFirstPersonBackground;

    @NotNull
    public final GeoModelProperties properties;

    public final float @NotNull [] initialState;

    // Native Access
    @SuppressWarnings("all")
    private long nativeId;

    // Native Access
    public GeoModel(GeoBone[] sortedBones, String[][] locatorHierarchy, boolean[] hasRendererFeature, @NotNull GeoModelProperties properties) {
        this.sortedBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(sortedBones));

        this.leftHandBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(locatorHierarchy[0]));
        this.rightHandBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(locatorHierarchy[1]));
        this.elytraBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(locatorHierarchy[2]));
        this.tacPistolBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(locatorHierarchy[3]));
        this.tacRifleBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(locatorHierarchy[4]));
        this.leftWaistBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(locatorHierarchy[5]));
        this.rightWaistBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(locatorHierarchy[6]));
        this.leftShoulderBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(locatorHierarchy[7]));
        this.rightShoulderBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(locatorHierarchy[8]));

        this.bladeBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(locatorHierarchy[9]));
        this.sheathBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(locatorHierarchy[10]));

        // 头部和背包，主要是兼容女仆的
        this.headBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(locatorHierarchy[11]));
        this.backpackBones = ObjectLists.unmodifiable(ObjectArrayList.wrap(locatorHierarchy[12]));

        hasFirstPersonLeftArm = hasRendererFeature[0];
        hasFirstPersonRightArm = hasRendererFeature[1];
        hasFirstPersonBackground = hasRendererFeature[2];

        this.properties = properties;

        this.initialState = new GeoModelState(this).inputState();
        CleanerUtil.ref(this, GeoModel::free);
    }

    @NotNull
    public List<GeoBone> getSortedBones() {
        return sortedBones;
    }

    public float @NotNull [] getInitialState() {
        return initialState;
    }

    private native void free();
}
