package com.elfmcys.ysm.geckolib3.geo.animated;

import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.buffer.annotation.Aligned;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoLocator;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.ysm.geckolib3.model.AnimatedGeoModel;
import com.elfmcys.ysm.natives.NativeObject;
import com.elfmcys.ysm.natives.render.NativeModelState;
import com.elfmcys.ysm.util.Closeable;
import com.elfmcys.ysm.util.ExposedShortArrayList;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.function.Consumer;


public final class GeoModelState implements Closeable {
    private static final int POSE_STRIDE = 128;
    private static final int NORMAL_OFFSET = 64;

    private final NativeObject nativeState = NativeModelState.create();
    private final ExposedShortArrayList activeLocatorBoneIndices = new ExposedShortArrayList(0);
    private final ReferenceArrayList<ShortArrayList> activeLocatorMap = new ReferenceArrayList<>(0);

    @Aligned(64)
    private NativeBuffer bonePose;
    private ByteBuffer nioBonePose;
    private GeoModel model;
    private int vertexCount;
    private boolean valid;

    public boolean extract(AnimatedGeoModel animatedModel) {
        invalidate();
        var model = animatedModel.getModel();
        var boneCount = model.sortedBones().size();
        ensureCapacity(boneCount);

        var locatorIndices = activeLocatorBoneIndices.getUnderlyingArray();
        var activeLocatorMap = this.activeLocatorMap;

        var packed = NativeModelState.extract(nativeState, model.bakedModel(),
                animatedModel.getBoneAttributes(), locatorIndices, bonePose);
        if (packed == -1) {
            return false;
        }

        var locatorSlotCount = model.locatorType().size();
        if (activeLocatorMap.size() != locatorSlotCount) {
            var oldSize = activeLocatorMap.size();
            var newSize = locatorSlotCount;
            activeLocatorMap.size(newSize);
            for (var i = oldSize; i < newSize; i++) {
                activeLocatorMap.set(i, new ExposedShortArrayList(2));
            }
        }
        for(var list : activeLocatorMap) {
            list.clear();
        }

        var extractedVertexCount = (int) packed;
        var extractedLocatorCount = (int) (packed >>> 32);
        if (extractedVertexCount < 0 || extractedLocatorCount < 0 ||
                extractedLocatorCount > boneCount) {
            return false;
        }

        var sortedBones = model.sortedBones();
        for (var i = 0; i < extractedLocatorCount; i++) {
            var boneIndex = Short.toUnsignedInt(locatorIndices[i]);
            if (boneIndex >= boneCount) {
                return false;
            }
            var locatorType = sortedBones.get(boneIndex).locatorType();
            if (locatorType != null) {
                activeLocatorMap.get(Byte.toUnsignedInt(locatorType.seq()) - 1).add((short) boneIndex);
            }
        }

        this.valid = true;
        this.vertexCount = extractedVertexCount;
        this.model = model;
        return true;
    }

    private void ensureCapacity(int requiredBoneCount) {
        if (requiredBoneCount < 0) {
            throw new IllegalArgumentException("Negative bone count");
        }
        if (activeLocatorBoneIndices.size() < requiredBoneCount) {
            activeLocatorBoneIndices.size(requiredBoneCount);
        }
        var requiredBytes = Math.multiplyExact(requiredBoneCount, POSE_STRIDE);
        if (bonePose == null || bonePose.size() < requiredBytes) {
            var oldSize = bonePose == null ? 0 : bonePose.size();
            var capacity = Math.max(POSE_STRIDE,
                    Math.max(requiredBytes, oldSize > Integer.MAX_VALUE / 2
                            ? requiredBytes : oldSize * 2));
            var replacement = NativeBuffer.allocate(capacity, 64);
            if (bonePose != null) {
                bonePose.close();
            }
            bonePose = replacement;
            nioBonePose = replacement.nio();
        }
    }

    public boolean isValid() {
        return valid;
    }

    public int getVertexCount() {
        requireValid();
        return vertexCount;
    }

    public NativeObject getNativeState() {
        requireValid();
        return nativeState;
    }

    public void visitLocatorGroup(GeoLocator locator, PoseStack poseStack,
                                  Consumer<PoseStack> visitor) {
        requireValid();
        if (model.locatorType() != locator.type()) {
            throw new IllegalArgumentException("locator type mismatch");
        }

        var backupPose = new Matrix4f(poseStack.last().pose());
        var backupNormal = new Matrix3f(poseStack.last().normal());
        var bonePose = new Matrix4f();
        var boneNormal = new Matrix3f();
        try {
            for (var boneIndexShort : activeLocatorMap.get(Byte.toUnsignedInt(locator.seq()) - 1)) {
                var boneIndex = Short.toUnsignedInt(boneIndexShort);
                var bone = model.sortedBones().get(boneIndex);

                var last = poseStack.last();
                getBonePose(boneIndex, bonePose, boneNormal);
                backupPose.mulAffine(bonePose, last.pose());
                backupNormal.mul(boneNormal, last.normal());

                var pivot = bone.pivot();
                poseStack.translate(pivot.x / 16, pivot.y / 16, pivot.z / 16);

                visitor.accept(poseStack);
            }
        } finally {
            var last = poseStack.last();
            last.pose().set(backupPose);
            last.normal().set(backupNormal);
        }
    }

    public int locatorGroupSize(GeoLocator locator) {
        requireValid();
        if (model.locatorType() != locator.type()) {
            throw new IllegalArgumentException("locator type mismatch");
        }
        return activeLocatorMap.get(Byte.toUnsignedInt(locator.seq())).size();
    }

    private void getBonePose(int boneIndex, Matrix4f pose, Matrix3f normal) {
        var offset = Math.multiplyExact(boneIndex, POSE_STRIDE);
        pose.set(offset, nioBonePose);
        normal.set(offset + NORMAL_OFFSET, nioBonePose);
    }

    private void requireValid() {
        if (!valid) {
            throw new IllegalStateException("GeoModelState has no valid extracted snapshot");
        }
    }

    private void invalidate() {
        valid = false;
        model = null;
        vertexCount = 0;
    }

    @Override
    public void close() {
        invalidate();
        if (bonePose != null) {
            bonePose.close();
            bonePose = null;
            nioBonePose = null;
        }
        nativeState.close();
    }
}
