package com.elfmcys.yesstevemodel.geckolib3.geo.render.built;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.geo.raw.pojo.GeoModelProperties;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.util.CleanerUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
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
    public final IntList leftHandBones;
    @NotNull
    public final List<IntList> extraLeftHandBones = new ObjectArrayList<>();
    @NotNull
    public final IntList rightHandBones;
    @NotNull
    public final List<IntList> extraRightHandBones = new ObjectArrayList<>();
    @NotNull
    public final List<IntList> passengerBones = new ObjectArrayList<>();
    @NotNull
    public final IntList elytraBones;
    @NotNull
    public final IntList tacPistolBones;
    @NotNull
    public final IntList tacRifleBones;
    @NotNull
    public final IntList leftWaistBones;
    @NotNull
    public final IntList rightWaistBones;
    @NotNull
    public final IntList leftShoulderBones;
    @NotNull
    public final IntList rightShoulderBones;

    @NotNull
    public final IntList bladeBones;
    @NotNull
    public final IntList sheathBones;

    @NotNull
    public final IntList headBones;
    @NotNull
    public final IntList backpackBones;

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

        this.leftHandBones = buildLocatorHierarchy(locatorHierarchy[0]);
        this.rightHandBones = buildLocatorHierarchy(locatorHierarchy[1]);
        this.elytraBones = buildLocatorHierarchy(locatorHierarchy[2]);
        this.tacPistolBones = buildLocatorHierarchy(locatorHierarchy[3]);
        this.tacRifleBones = buildLocatorHierarchy(locatorHierarchy[4]);
        this.leftWaistBones = buildLocatorHierarchy(locatorHierarchy[5]);
        this.rightWaistBones = buildLocatorHierarchy(locatorHierarchy[6]);
        this.leftShoulderBones = buildLocatorHierarchy(locatorHierarchy[7]);
        this.rightShoulderBones = buildLocatorHierarchy(locatorHierarchy[8]);

        this.bladeBones = buildLocatorHierarchy(locatorHierarchy[9]);
        this.sheathBones = buildLocatorHierarchy(locatorHierarchy[10]);

        // 头部和背包，主要是兼容女仆的
        this.headBones = buildLocatorHierarchy(locatorHierarchy[11]);
        this.backpackBones = buildLocatorHierarchy(locatorHierarchy[12]);

        // 13-19 是额外副手物品
        for (int i = 13; i <= 19; i++) {
            String[] extraLocators = locatorHierarchy[i];
            if (extraLocators.length > 0) {
                extraLeftHandBones.add(buildLocatorHierarchy(extraLocators));
            }
        }
        // 20-26 是额外主手物品
        for (int i = 20; i <= 26; i++) {
            String[] extraLocators = locatorHierarchy[i];
            if (extraLocators.length > 0) {
                extraRightHandBones.add(buildLocatorHierarchy(extraLocators));
            }
        }

        // 27-34 是乘客点位
        for (int i = 27; i <= 34; i++) {
            String[] extraLocators = locatorHierarchy[i];
            if (extraLocators.length > 0) {
                passengerBones.add(buildLocatorHierarchy(extraLocators));
            }
        }

        hasFirstPersonLeftArm = hasRendererFeature[0];
        hasFirstPersonRightArm = hasRendererFeature[1];
        hasFirstPersonBackground = hasRendererFeature[2];

        this.properties = properties;

        this.initialState = new GeoModelState(this).inputState();
        CleanerUtil.ref(this, GeoModel::free);
    }

    private IntList buildLocatorHierarchy(String[] hierarchy) {
        var list = new IntArrayList(hierarchy.length);
        for (var name : hierarchy) {
            list.add(StringPool.computeIfAbsent(name));
        }
        return IntLists.unmodifiable(list);
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
