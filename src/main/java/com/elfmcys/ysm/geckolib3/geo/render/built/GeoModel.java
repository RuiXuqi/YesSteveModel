package com.elfmcys.ysm.geckolib3.geo.render.built;

import com.elfmcys.ysm.natives.NativeObject;
import com.elfmcys.ysm.natives.render.NativeBakedModel;
import mixel.asset.model.data.GeoModelOuterClass;
import com.elfmcys.ysm.util.Closeable;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import it.unimi.dsi.fastutil.objects.ReferenceLists;

import java.util.Objects;
import java.util.function.IntFunction;

public class GeoModel implements Closeable {
    private final ReferenceList<GeoBone> sortedBones;
    private final GeoLocatorType locatorType;
    private final ReferenceList<ReferenceArrayList<GeoBone>> locatorMap;
    private final NativeObject bakedModel;

    public GeoModel(GeoModelOuterClass.GeoModel model,
                    GeoLocatorType locatorType,
                    NativeBakedModel.ReadResult bakedModel) {
        this(boneCount(model), index -> model.getBones().get(index),
                bakedModel.sortedBoneIndices(), locatorType,
                bakedModel.bakedModel());
    }

    public GeoModel(GeoModelOuterClass.GeoModelIndex model,
                    GeoLocatorType locatorType,
                    NativeBakedModel.ReadResult bakedModel) {
        this(boneCount(model), index -> model.getBones().get(index),
                bakedModel.sortedBoneIndices(), locatorType,
                bakedModel.bakedModel());
    }

    private GeoModel(int boneCount,
                     IntFunction<GeoModelOuterClass.Bone> boneByIndex,
                     short[] sortedBoneIndices, GeoLocatorType locatorType,
                     NativeObject bakedModel) {
        Objects.requireNonNull(sortedBoneIndices, "sortedBoneIndices");
        this.locatorType = Objects.requireNonNull(locatorType, "locatorType");
        this.bakedModel = Objects.requireNonNull(bakedModel, "bakedModel");
        if (sortedBoneIndices.length != boneCount) {
            throw new IllegalArgumentException("Bone index count mismatch");
        }

        var locatorMap = new ReferenceArrayList<ReferenceArrayList<GeoBone>>(locatorType.size());
        for (int i = 0; i < locatorType.size(); i++) {
            locatorMap.add(new ReferenceArrayList<>(2));
        }
        var sortedBones = new ReferenceArrayList<GeoBone>(boneCount);
        for (var sortedIndex = 0; sortedIndex < boneCount; sortedIndex++) {
            var originalIndex = Short.toUnsignedInt(sortedBoneIndices[sortedIndex]);
            if (originalIndex >= boneCount) {
                throw new IllegalArgumentException("Invalid sorted bone indices");
            }
            var boneData = boneByIndex.apply(originalIndex);
            var locator = locatorType.getByBoneName(boneData.getName());
            var bone = new GeoBone(boneData, locator);
            sortedBones.add(bone);
            if (locator != null) {
                 locatorMap.get(locator.seq() - 1).add(bone);
            }
        }
        this.sortedBones = ReferenceLists.unmodifiable(sortedBones);
        this.locatorMap = ReferenceLists.unmodifiable(locatorMap);
    }

    private static int boneCount(GeoModelOuterClass.GeoModel model) {
        return model.hasBones() ? model.getBones().length() : 0;
    }

    private static int boneCount(GeoModelOuterClass.GeoModelIndex model) {
        return model.hasBones() ? model.getBones().length() : 0;
    }

    public ReferenceList<GeoBone> sortedBones() {
        return sortedBones;
    }

    public GeoLocatorType locatorType() {
        return locatorType;
    }

    public ReferenceList<ReferenceArrayList<GeoBone>> locatorMap() {
        return locatorMap;
    }

    public NativeObject bakedModel() {
        return bakedModel;
    }

    @Override
    public void close() {
        bakedModel.close();
    }
}
