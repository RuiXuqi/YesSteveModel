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
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class NativeBakedModel extends NativeObject {
    private static final int BONE_INFO_INT_COUNT = 4;
    private static final int CUBE_DATA_FLOAT_COUNT = 133;
    private static final int CUBE_VERTEX_COUNT = 8;
    private static final int CUBE_QUAD_COUNT = 6;
    private static final int CUBE_POSITION_FLOAT_COUNT = 3;
    private static final int QUAD_DATA_FLOAT_COUNT = 18;
    private static final int QUAD_DATA_OFFSET = 24;
    private static final int CUBE_COUNTS_OFFSET = 132;

    private static final ThreadLocal<int[]> INT_BUFFER =
            ThreadLocal.withInitial(() -> new int[0]);
    private static final ThreadLocal<float[]> FLOAT_BUFFER =
            ThreadLocal.withInitial(() -> new float[0]);

    public record Info(int boneCount, boolean guiNoShadow, boolean hasPbr) {
    }

    public record BonePartitionInfo(int cubeCount,
                                    int vertexCount,
                                    int vertexCountAfterCulling) {
    }

    public record BoneInfo(BonePartitionInfo cutout,
                           BonePartitionInfo cutoutNoCulling,
                           BonePartitionInfo translucent,
                           BonePartitionInfo translucentCulling) {
    }

    public record QuadData(Vector3f normal,
                           Vector4f tangent,
                           Vector2f[] uv,
                           int vertex0,
                           int vertex1,
                           int vertex2,
                           int vertex3,
                           float planeD,
                           float windingSign) {
    }

    public record CubeData(Vector3f[] positions,
                           QuadData[] quads,
                           int quadCount,
                           int quadCountAfterCulling) {
    }

    public record BakeResult(NativeBuffer bakedData, short[] sortedBoneIndices) {
    }

    public record ReadResult(NativeBakedModel bakedModel, short[] sortedBoneIndices) {
    }

    NativeBakedModel(long ptr) {
        super(ptr);
    }

    public static int capability() {
        return nCapability();
    }

    public Info getInfo() {
        var data = nGetInfo(get());
        if (data == -1) {
            throw new RuntimeException("Failed to get BakedModel info");
        }
        return new Info(
                (int) (data & 0xffff),
                (data & (1L << 16)) != 0,
                (data & (1L << 17)) != 0);
    }

    public BoneInfo[] getBoneInfo(int boneIndex, int boneCount) {
        var requiredSize = checkedArraySize(boneCount, BONE_INFO_INT_COUNT);
        var data = getIntBuffer(requiredSize);
        if (!nGetBoneInfo(get(), boneIndex, boneCount, data)) {
            throw new RuntimeException("Failed to get BakedModel bone info");
        }

        var result = new BoneInfo[boneCount];
        for (var i = 0; i < result.length; ++i) {
            var offset = i * BONE_INFO_INT_COUNT;
            result[i] = new BoneInfo(
                    unpackBonePartitionInfo(data[offset]),
                    unpackBonePartitionInfo(data[offset + 1]),
                    unpackBonePartitionInfo(data[offset + 2]),
                    unpackBonePartitionInfo(data[offset + 3]));
        }
        return result;
    }

    public CubeData[] getCubeData(int boneIndex, int bonePartition,
                                  int cubeIndex, int cubeCount) {
        var requiredSize = checkedArraySize(cubeCount, CUBE_DATA_FLOAT_COUNT);
        var data = getFloatBuffer(requiredSize);
        if (!nGetCubeData(get(), boneIndex, bonePartition, cubeIndex,
                cubeCount, data)) {
            throw new RuntimeException("Failed to get BakedModel cube data");
        }

        var result = new CubeData[cubeCount];
        for (var i = 0; i < result.length; ++i) {
            result[i] = unpackCubeData(data, i * CUBE_DATA_FLOAT_COUNT);
        }
        return result;
    }

    private static int checkedArraySize(int count, int stride) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative element count");
        }
        return Math.multiplyExact(count, stride);
    }

    private static int[] getIntBuffer(int requiredSize) {
        var data = INT_BUFFER.get();
        if (data.length < requiredSize) {
            data = new int[requiredSize];
            INT_BUFFER.set(data);
        }
        return data;
    }

    private static float[] getFloatBuffer(int requiredSize) {
        var data = FLOAT_BUFFER.get();
        if (data.length < requiredSize) {
            data = new float[requiredSize];
            FLOAT_BUFFER.set(data);
        }
        return data;
    }

    private static BonePartitionInfo unpackBonePartitionInfo(int data) {
        return new BonePartitionInfo(
                data & 0xffff,
                data >>> 16 & 0xff,
                data >>> 24);
    }

    private static CubeData unpackCubeData(float[] data, int offset) {
        var positions = new Vector3f[CUBE_VERTEX_COUNT];
        for (var i = 0; i < positions.length; ++i) {
            var positionOffset = offset + i * CUBE_POSITION_FLOAT_COUNT;
            positions[i] = new Vector3f(
                    data[positionOffset],
                    data[positionOffset + 1],
                    data[positionOffset + 2]);
        }

        var counts = Float.floatToRawIntBits(data[offset +
                CUBE_COUNTS_OFFSET]);
        var quadCount = counts & 0xff;
        var quadCountAfterCulling = counts >>> 8 & 0xff;
        if (quadCount > CUBE_QUAD_COUNT ||
                quadCountAfterCulling > quadCount) {
            throw new IllegalStateException("Invalid native cube data");
        }

        var quads = new QuadData[quadCount];
        for (var i = 0; i < quads.length; ++i) {
            var quadOffset = offset + QUAD_DATA_OFFSET +
                    i * QUAD_DATA_FLOAT_COUNT;
            var uv = new Vector2f[4];
            for (var vertex = 0; vertex < uv.length; ++vertex) {
                var uvOffset = quadOffset + 7 + vertex * 2;
                uv[vertex] = new Vector2f(data[uvOffset], data[uvOffset + 1]);
            }
            var vertexIndices = Float.floatToRawIntBits(data[quadOffset + 15]);
            quads[i] = new QuadData(
                    new Vector3f(data[quadOffset], data[quadOffset + 1],
                            data[quadOffset + 2]),
                    new Vector4f(data[quadOffset + 3], data[quadOffset + 4],
                            data[quadOffset + 5], data[quadOffset + 6]),
                    uv,
                    vertexIndices & 0xff,
                    vertexIndices >>> 8 & 0xff,
                    vertexIndices >>> 16 & 0xff,
                    vertexIndices >>> 24,
                    data[quadOffset + 16],
                    data[quadOffset + 17]);
        }
        return new CubeData(positions, quads, quadCount,
                quadCountAfterCulling);
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
        return new ReadResult(new NativeBakedModel(ptr), sortedBoneIndices);
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

    private static native long nGetInfo(long bakedModel);

    private static native boolean nGetBoneInfo(long bakedModel, int boneIndex,
                                               int boneCount, int[] dst);

    private static native boolean nGetCubeData(long bakedModel, int boneIndex,
                                               int bonePartition, int cubeIndex,
                                               int cubeCount, float[] dst);
}
