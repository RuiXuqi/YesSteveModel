package com.elfmcys.yesstevemodel.geckolib3.model;

import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class GeoModelState {
    private static final int INPUT_STATE_STRIDE = 12;
    private static final int OUTPUT_STATE_STRIDE = 4;

    private final Object2ReferenceMap<String, IBone> boneMap;

    private final float[] inputState;
    private final float[] outputState;
    private final GeoModel model;

    @NotNull
    private final List<IBone> leftHandBones;
    @NotNull
    private final List<IBone> rightHandBones;
    @NotNull
    private final List<IBone> elytraBones;
    @NotNull
    private final List<IBone> tacPistolBones;
    @NotNull
    private final List<IBone> tacRifleBones;
    @NotNull
    private final List<IBone> leftWaistBones;
    @NotNull
    private final List<IBone> rightWaistBones;
    @NotNull
    private final List<IBone> leftShoulderBones;
    @NotNull
    private final List<IBone> rightShoulderBones;
    @NotNull
    private final List<IBone> bladeBones;
    @NotNull
    private final List<IBone> sheathBones;
    @Nullable
    private final IBone firstPersonHead;
    @Nullable
    private final IBone firstPersonViewLocator;

    public GeoModelState(GeoModel model) {
        this.model = model;

        List<GeoBone> sortedBones = model.getSortedBones();

        this.inputState = new float[INPUT_STATE_STRIDE * sortedBones.size()];
        this.outputState = new float[OUTPUT_STATE_STRIDE * sortedBones.size()];

        Object2ReferenceOpenHashMap<String, IBone> boneMap = new Object2ReferenceOpenHashMap<>(sortedBones.size());
        for (int i = 0; i < sortedBones.size(); i++) {
            GeoBone bone = sortedBones.get(i);
            boneMap.put(bone.name(), new GeoBoneState(bone, inputState, i * INPUT_STATE_STRIDE, outputState, i * OUTPUT_STATE_STRIDE));
        }
        this.boneMap = Object2ReferenceMaps.unmodifiable(boneMap);

        leftHandBones = findBones(model.leftHandBones);
        rightHandBones = findBones(model.rightHandBones);
        elytraBones = findBones(model.elytraBones);
        tacPistolBones = findBones(model.tacPistolBones);
        tacRifleBones = findBones(model.tacRifleBones);
        leftWaistBones = findBones(model.leftWaistBones);
        rightWaistBones = findBones(model.rightWaistBones);
        leftShoulderBones = findBones(model.leftShoulderBones);
        rightShoulderBones = findBones(model.rightShoulderBones);
        bladeBones = findBones(model.bladeBones);
        sheathBones = findBones(model.sheathBones);
        firstPersonHead = boneMap.get("AllHead");
        firstPersonViewLocator = boneMap.get("ViewLocator");
    }

    @NotNull
    private List<IBone> findBones(@NotNull List<String> boneNames) {
        ReferenceArrayList<IBone> list = new ReferenceArrayList<>(boneNames.size());
        for (String boneName : boneNames) {
            list.add(boneMap.get(boneName));
        }
        return ReferenceLists.unmodifiable(list);
    }

    public float[] inputState() {
        return inputState;
    }

    public float[] outputState() {
        return outputState;
    }

    public Map<String, IBone> boneMap() {
        return boneMap;
    }

    public GeoModel model() {
        return model;
    }

    @NotNull
    public List<IBone> leftHandBones() {
        return leftHandBones;
    }

    @NotNull
    public List<IBone> rightHandBones() {
        return rightHandBones;
    }

    @NotNull
    public List<IBone> elytraBones() {
        return elytraBones;
    }

    @NotNull
    public List<IBone> tacPistolBones() {
        return tacPistolBones;
    }

    @NotNull
    public List<IBone> tacRifleBones() {
        return tacRifleBones;
    }

    @NotNull
    public List<IBone> leftWaistBones() {
        return leftWaistBones;
    }

    @NotNull
    public List<IBone> rightWaistBones() {
        return rightWaistBones;
    }

    @NotNull
    public List<IBone> leftShoulderBones() {
        return leftShoulderBones;
    }

    @NotNull
    public List<IBone> rightShoulderBones() {
        return rightShoulderBones;
    }

    public @NotNull List<IBone> bladeBones() {
        return bladeBones;
    }

    public @NotNull List<IBone> sheathBones() {
        return sheathBones;
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
