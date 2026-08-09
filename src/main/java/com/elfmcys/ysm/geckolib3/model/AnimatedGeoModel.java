package com.elfmcys.ysm.geckolib3.model;

import com.elfmcys.ysm.geckolib3.core.molang.util.StringPool;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoLocator;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class AnimatedGeoModel {
    public static final int BONE_ATTRIBUTE_COUNT = 14;
    private static final int ALL_HEAD_NAME = StringPool.computeIfAbsent("AllHead");
    private static final int VIEW_LOCATOR_NAME = StringPool.computeIfAbsent("ViewLocator");

    private final Int2ReferenceMap<AnimatedGeoBone> boneMap;
    private final ReferenceList<AnimatedGeoBone> sortedBones;
    private final ReferenceArrayList<ReferenceArrayList<AnimatedGeoBone>> locatorMap;
    private final float[] boneAttributes;

    private GeoModel model;

    @Nullable
    private final AnimatedGeoBone firstPersonHead;
    @Nullable
    private final AnimatedGeoBone firstPersonViewLocator;

    /**
     * 仅用于 TLM 定位组
     * <p>
     * FIXME: 其实不应该这样耦合的
     */
    @Nullable
    private Object tlmAnimatedGeoModel = null;

    public AnimatedGeoModel(GeoModel model) {
        this.model = model;

        var sortedBones = model.sortedBones();
        var boneMap = new Int2ReferenceOpenHashMap<AnimatedGeoBone>(sortedBones.size());
        var boneList = new ReferenceArrayList<AnimatedGeoBone>(sortedBones.size());
        this.boneAttributes = new float[sortedBones.size() * BONE_ATTRIBUTE_COUNT];
        for (var boneIndex = 0; boneIndex < sortedBones.size(); boneIndex++) {
            var animated = new AnimatedGeoBone(sortedBones.get(boneIndex),
                    boneAttributes, boneIndex);
            boneMap.put(animated.getPooledName(), animated);
            boneList.add(animated);
        }
        this.boneMap = Int2ReferenceMaps.unmodifiable(boneMap);
        this.sortedBones = ReferenceLists.unmodifiable(boneList);
        this.locatorMap = new ReferenceArrayList<>(model.locatorMap().size());
        for (var rawGroup : model.locatorMap()) {
            var group = new ReferenceArrayList<AnimatedGeoBone>(rawGroup.size());
            for (var rawBone : rawGroup) {
                group.add(this.boneMap.get(rawBone.pooledName()));
            }
            this.locatorMap.add(group);
        }

        firstPersonHead = boneMap.get(ALL_HEAD_NAME);
        firstPersonViewLocator = boneMap.get(VIEW_LOCATOR_NAME);
    }

    public GeoModel getModel() {
        return model;
    }

    public void setModelInplace(GeoModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public Int2ReferenceMap<AnimatedGeoBone> getBoneMap() {
        return boneMap;
    }

    public ReferenceList<AnimatedGeoBone> getSortedBones() {
        return sortedBones;
    }

    public float[] getBoneAttributes() {
        return boneAttributes;
    }

    @Nullable
    public AnimatedGeoBone getFirstPersonHead() {
        return firstPersonHead;
    }

    @Nullable
    public AnimatedGeoBone getFirstPersonViewLocator() {
        return firstPersonViewLocator;
    }

    @NotNull
    public ReferenceArrayList<AnimatedGeoBone> locatorGroup(GeoLocator type) {
        return locatorMap.get(type.seq() - 1);
    }

    // TODO
//    @SuppressWarnings("unchecked")
//    public <T> T getTlmAnimatedGeoModel() {
//        if (this.tlmAnimatedGeoModel == null) {
//            // FIXME: 有可能会触发类加载？
//            this.tlmAnimatedGeoModel = TlmConverterHelper.convertToTlmAnimatedModel(this);
//        }
//        return (T) this.tlmAnimatedGeoModel;
//    }
}
