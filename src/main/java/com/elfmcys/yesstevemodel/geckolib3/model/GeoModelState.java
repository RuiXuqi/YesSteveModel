package com.elfmcys.yesstevemodel.geckolib3.model;

import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import it.unimi.dsi.fastutil.objects.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class GeoModelState {
    private static final int STATE_STRIDE = 12;

    private final Object2ReferenceMap<String, IBone> boneMap;
    private final float[] state;
    private final GeoModel model;

    @Nonnull
    private final List<IBone> leftHandBones;
    @Nonnull
    private final List<IBone> rightHandBones;
    @Nonnull
    private final List<IBone> elytraBones;
    @Nonnull
    private final List<IBone> tacPistolBones;
    @Nonnull
    private final List<IBone> tacRifleBones;
    @Nullable
    private final IBone firstPersonHead;
    @Nullable
    private final IBone firstPersonViewLocator;

    public GeoModelState(GeoModel model) {
        this.model = model;

        List<GeoBone> sortedBones = model.getSortedBones();
        this.state = new float[STATE_STRIDE * sortedBones.size()];
        Object2ReferenceOpenHashMap<String, IBone> boneMap = new Object2ReferenceOpenHashMap<>(sortedBones.size());
        for (int i = 0; i < sortedBones.size(); i++) {
            GeoBone bone = sortedBones.get(i);
            boneMap.put(bone.name(), new GeoBoneState(bone, state, i * STATE_STRIDE));
        }
        this.boneMap = Object2ReferenceMaps.unmodifiable(boneMap);

        leftHandBones = findBones(model.leftHandBones);
        rightHandBones = findBones(model.rightHandBones);
        elytraBones = findBones(model.elytraBones);
        tacPistolBones = findBones(model.tacPistolBones);
        tacRifleBones = findBones(model.tacRifleBones);
        firstPersonHead = findBone(model.firstPersonHead);
        firstPersonViewLocator = findBone(model.firstPersonViewLocator);
    }

    @Nonnull
    private List<IBone> findBones(@Nonnull List<GeoBone> bones) {
        ReferenceArrayList<IBone> list = new ReferenceArrayList<>(bones.size());
        for(GeoBone bone : bones) {
            list.add(boneMap.get(bone.name()));
        }
        return ReferenceLists.unmodifiable(list);
    }

    @Nullable
    private IBone findBone(@Nullable GeoBone bone) {
        if(bone == null) {
            return null;
        }
        return boneMap.get(bone.name());
    }

    public float[] state() {
        return state;
    }

    public Map<String, IBone> boneMap() {
        return boneMap;
    }

    public GeoModel model() {
        return model;
    }

    @Nonnull
    public List<IBone> leftHandBones() {
        return leftHandBones;
    }

    @Nonnull
    public List<IBone> rightHandBones() {
        return rightHandBones;
    }

    @Nonnull
    public List<IBone> elytraBones() {
        return elytraBones;
    }

    @Nonnull
    public List<IBone> tacPistolBones() {
        return tacPistolBones;
    }

    @Nonnull
    public List<IBone> tacRifleBones() {
        return tacRifleBones;
    }

    @Nullable
    public IBone firstPersonHead() {
        return firstPersonHead;
    }

    @Nullable
    public IBone firstPersonViewLocator() {
        return firstPersonViewLocator;
    }
}
