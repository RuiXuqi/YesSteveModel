package com.elfmcys.ysm.natives.render;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.buffer.annotation.Owned;
import com.elfmcys.ysm.mixin.client.NativeImageAccessor;
import com.elfmcys.ysm.natives.NativeObject;
import com.elfmcys.ysm.natives.buffer.BufferArgument;
import com.elfmcys.ysm.natives.buffer.NativeHeapBuffer;
import mixel.asset.model.data.GeoModelOuterClass;
import com.elfmcys.ysm.util.ProtoUtil;
import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class NativeBakedModel {
    public record BakeResult(NativeBuffer bakedData, short[] sortedBoneIndices) {
    }

    public record ReadResult(NativeObject bakedModel, short[] sortedBoneIndices) {
    }

    private NativeBakedModel() {
    }

    public static int capability() {
        return nCapability();
    }

    @Owned
    public static BakeResult bake(GeoModelOuterClass.GeoModel model,
                                  NativeImage texture, int originVersion,
                                  boolean forceCulling, boolean forceTranslucent,
                                  boolean hasPbr) throws IOException {
        try (var modelData = ArrayBuffer.allocateWithScope(model.getSerializedSize())) {
            model.writeTo(ProtoUtil.sink(modelData.get()));
            return bake(modelData.get(), boneCount(model), texture, originVersion,
                    forceCulling, forceTranslucent, hasPbr);
        }
    }

    @Owned
    public static BakeResult bake(UniBuffer modelData, int boneCount,
                                  NativeImage texture, int originVersion,
                                  boolean forceCulling, boolean forceTranslucent,
                                  boolean hasPbr) {
        if (texture.format() != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException("Texture format not supported");
        }
        var accessor = (NativeImageAccessor) (Object) texture;
        return bake(modelData, boneCount, accessor.ysm$pixels(),
                texture.getWidth(), texture.getHeight(), originVersion,
                forceCulling, forceTranslucent, hasPbr);
    }

    @Owned
    public static BakeResult bake(UniBuffer modelData, int boneCount,
                                  long texturePtr, int textureWidth,
                                  int textureHeight, int originVersion,
                                  boolean hasPbr) {
        return bake(modelData, boneCount, texturePtr, textureWidth,
                textureHeight, originVersion, false, false, hasPbr);
    }

    @Owned
    public static BakeResult bake(UniBuffer modelData, int boneCount,
                                  long texturePtr, int textureWidth,
                                  int textureHeight, int originVersion,
                                  boolean forceCulling,
                                  boolean forceTranslucent, boolean hasPbr) {
        validateBoneCount(boneCount);
        if (originVersion < 0 || originVersion > 0xffff) {
            throw new IllegalArgumentException("Invalid origin version");
        }
        var sortedBoneIndices = new short[boneCount];
        var input = BufferArgument.packInput(modelData);
        var output = nBake(input.obj(), input.flags(), sortedBoneIndices,
                texturePtr, textureWidth, textureHeight,
                packBakeOptions(originVersion, forceCulling, forceTranslucent,
                        hasPbr));
        if (output == null) {
            throw new RuntimeException("Failed to bake model");
        }
        return new BakeResult(new NativeHeapBuffer(output), sortedBoneIndices);
    }

    @Owned
    public static ReadResult read(UniBuffer buffer, int boneCount) {
        validateBoneCount(boneCount);
        var sortedBoneIndices = new short[boneCount];
        var args = BufferArgument.packInput(buffer);
        var ptr = nRead(args.obj(), args.flags(), sortedBoneIndices);
        if (ptr == 0) {
            throw new RuntimeException("Failed to read BakedModel");
        }
        return new ReadResult(new NativeObject(ptr), sortedBoneIndices);
    }

    public static boolean tryBake(UniBuffer modelData, NativeImage texture,
                                  int originVersion, boolean forceCulling,
                                  boolean forceTranslucent, boolean hasPbr) {
        if (texture.format() != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException("Texture format not supported");
        }
        var accessor = (NativeImageAccessor) (Object) texture;
        return tryBake(modelData, accessor.ysm$pixels(), texture.getWidth(),
                texture.getHeight(), originVersion, forceCulling,
                forceTranslucent, hasPbr);
    }

    public static boolean tryBake(GeoModelOuterClass.GeoModel model,
                                  NativeBuffer texturePixels,
                                  int textureWidth, int textureHeight,
                                  int originVersion, boolean forceCulling,
                                  boolean forceTranslucent, boolean hasPbr)
            throws IOException {
        try (var modelData = ArrayBuffer.allocateWithScope(model.getSerializedSize())) {
            model.writeTo(ProtoUtil.sink(modelData.get()));
            return tryBake(modelData.get(), texturePixels.ptr(), textureWidth,
                    textureHeight, originVersion, forceCulling,
                    forceTranslucent, hasPbr);
        }
    }

    public static boolean tryBake(UniBuffer modelData, long texturePtr,
                                  int textureWidth, int textureHeight,
                                  int originVersion, boolean forceCulling,
                                  boolean forceTranslucent, boolean hasPbr) {
        if (originVersion < 0 || originVersion > 0xffff) {
            throw new IllegalArgumentException("Invalid origin version");
        }
        var input = BufferArgument.packInput(modelData);
        return nTryBake(input.obj(), input.flags(), texturePtr, textureWidth,
                textureHeight, packBakeOptions(originVersion, forceCulling,
                        forceTranslucent, hasPbr));
    }

    private static int boneCount(GeoModelOuterClass.GeoModel model) {
        return model.hasBones() ? model.getBones().length() : 0;
    }

    private static void validateBoneCount(int boneCount) {
        if (boneCount < 0 || boneCount > 0x10000) {
            throw new IllegalArgumentException("Invalid bone count");
        }
    }

    private static long packBakeOptions(int originVersion, boolean forceCulling,
                                        boolean forceTranslucent, boolean hasPbr) {
        long options = originVersion;
        if (forceCulling) {
            options |= 1L << 16;
        }
        if (forceTranslucent) {
            options |= 1L << 17;
        }
        if (hasPbr) {
            options |= 1L << 18;
        }
        return options;
    }

    private static native ByteBuffer nBake(Object modelDataBuf,
                                           long modelDataFlags,
                                           short[] sortedBoneIndices,
                                           long texturePtr, int textureWidth,
                                           int textureHeight,
                                           long bakeOptions);

    private static native boolean nTryBake(Object modelDataBuf,
                                           long modelDataFlags,
                                           long texturePtr, int textureWidth,
                                           int textureHeight,
                                           long bakeOptions);

    private static native int nCapability();

    private static native long nRead(Object bakedDataBuf, long bakedDataFlags,
                                     short[] sortedBoneIndices);
}
