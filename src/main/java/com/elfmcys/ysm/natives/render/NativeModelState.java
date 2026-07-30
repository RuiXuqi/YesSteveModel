package com.elfmcys.ysm.natives.render;

import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.natives.NativeObject;

public final class NativeModelState {
    private NativeModelState() {
    }

    public static NativeObject create() {
        var ptr = nCreate();
        if (ptr == 0) {
            throw new RuntimeException("Failed to create ModelState");
        }
        return new NativeObject(ptr);
    }

    public static long extract(NativeObject state, NativeObject bakedModel,
                               float[] boneAttributes, short[] locatorBoneIndices,
                               NativeBuffer bonePose) {
        return nExtract(state.get(), bakedModel.get(), boneAttributes,
                locatorBoneIndices, bonePose.ptr(), bonePose.size());
    }

    private static native long nCreate();

    private static native long nExtract(long state, long bakedModel,
                                        float[] boneAttributes,
                                        short[] locatorBoneIndices,
                                        long bonePosePtr, long bonePoseCapacity);
}
