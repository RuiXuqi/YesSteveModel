package com.elfmcys.ysm.geckolib3.model;

import com.elfmcys.ysm.client.compat.touhoulittlemaid.util.TlmConverterHelper;
import com.elfmcys.ysm.geckolib3.core.molang.util.StringPool;
import com.elfmcys.ysm.geckolib3.core.processor.IBone;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoBone;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GeoModelState {
    private static final int INPUT_STATE_STRIDE = 12;
    private static final int OUTPUT_STATE_STRIDE = 4;

    private static final int ALL_HEAD_NAME = StringPool.computeIfAbsent("AllHead");
    private static final int VIEW_LOCATOR_NAME = StringPool.computeIfAbsent("ViewLocator");

    private final Int2ReferenceMap<IBone> boneMap;

    private final float[] inputState;
    private final float[] outputState;
    private final GeoModel model;

    @NotNull
    private final List<IBone> headBones;
    @NotNull
    private final List<IBone> leftHandBones;
    @NotNull
    private final List<List<IBone>> extraLeftHandBones = new ReferenceArrayList<>();
    @NotNull
    private final List<IBone> rightHandBones;
    @NotNull
    private final List<List<IBone>> extraRightHandBones = new ReferenceArrayList<>();
    @NotNull
    private final List<List<IBone>> passengerBones = new ReferenceArrayList<>();
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
    @NotNull
    private final List<IBone> backpackBones;
    @Nullable
    private final IBone firstPersonHead;
    @Nullable
    private final IBone firstPersonViewLocator;

    /**
     * 仅用于 TLM 定位组
     * <p>
     * FIXME: 其实不应该这样耦合的
     */
    @Nullable
    private Object tlmAnimatedGeoModel = null;

    public GeoModelState(GeoModel model) {
        this.model = model;

        List<GeoBone> sortedBones = model.getSortedBones();

        this.inputState = new float[INPUT_STATE_STRIDE * sortedBones.size()];
        this.outputState = new float[OUTPUT_STATE_STRIDE * sortedBones.size()];

        Int2ReferenceOpenHashMap<IBone> boneMap = new Int2ReferenceOpenHashMap<>(sortedBones.size());
        for (int i = 0; i < sortedBones.size(); i++) {
            GeoBone bone = sortedBones.get(i);
            boneMap.put(bone.pooledName(), new GeoBoneState(bone, inputState, i * INPUT_STATE_STRIDE, outputState, i * OUTPUT_STATE_STRIDE));
        }
        this.boneMap = Int2ReferenceMaps.unmodifiable(boneMap);

        headBones = findBones(model.headBones);
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
        backpackBones = findBones(model.backpackBones);
        firstPersonHead = boneMap.get(ALL_HEAD_NAME);
        firstPersonViewLocator = boneMap.get(VIEW_LOCATOR_NAME);

        model.extraLeftHandBones.forEach(list -> extraLeftHandBones.add(findBones(list)));
        model.extraRightHandBones.forEach(list -> extraRightHandBones.add(findBones(list)));
        model.passengerBones.forEach(list -> passengerBones.add(findBones(list)));
    }

    @NotNull
    private List<IBone> findBones(@NotNull IntList boneNames) {
        ReferenceArrayList<IBone> list = new ReferenceArrayList<>(boneNames.size());
        boneNames.forEach(name -> list.add(boneMap.get(name)));
        return ReferenceLists.unmodifiable(list);
    }

    public float[] inputState() {
        return inputState;
    }

    public float[] outputState() {
        return outputState;
    }

    public Int2ReferenceMap<IBone> boneMap() {
        return boneMap;
    }

    public GeoModel model() {
        return model;
    }

    @NotNull
    public List<IBone> leftHandBones() {
        return leftHandBones;
    }

    public @NotNull List<List<IBone>> extraLeftHandBones() {
        return extraLeftHandBones;
    }

    @NotNull
    public List<IBone> rightHandBones() {
        return rightHandBones;
    }

    public @NotNull List<List<IBone>> extraRightHandBones() {
        return extraRightHandBones;
    }

    public @NotNull List<List<IBone>> passengerBones() {
        return passengerBones;
    }

    @NotNull
    public List<IBone> elytraBones() {
        return elytraBones;
    }

    @NotNull
    public List<IBone> backpackBones() {
        return backpackBones;
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

    public List<IBone> headBones() {
        return headBones;
    }

    @SuppressWarnings("unchecked")
    public <T> T getTlmAnimatedGeoModel() {
        if (this.tlmAnimatedGeoModel == null) {
            // FIXME: 有可能会触发类加载？
            this.tlmAnimatedGeoModel = TlmConverterHelper.convertToTlmAnimatedModel(this);
        }
        return (T) this.tlmAnimatedGeoModel;
    }
}
