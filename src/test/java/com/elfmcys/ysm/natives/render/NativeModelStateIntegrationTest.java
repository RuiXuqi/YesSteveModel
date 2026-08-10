package com.elfmcys.ysm.natives.render;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.geckolib3.model.AnimatedGeoModel;
import com.elfmcys.ysm.testutil.NativeLibraryExtension;
import com.elfmcys.ysm.util.ProtoUtil;
import mixel.asset.model.data.GeoModelOuterClass;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "YSM_NATIVE_PATH", matches = ".+")
@ExtendWith(NativeLibraryExtension.class)
class NativeModelStateIntegrationTest {
    @Test
    void extractsQuadUv() throws Exception {
        var cube = GeoModelOuterClass.CubeLegacy.newInstance()
                .setFaceCount(1)
                .addAllPos(0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0)
                .addAllPosIndices(0, 1, 2, 3)
                .addAllUv(0, 0, 0.5f, 0, 0.5f, 0.5f, 0, 0.5f)
                .addAllUvIndices(0, 1, 2, 3)
                .addAllNormal(0, 0, 1);
        var cubes = GeoModelOuterClass.Cubes.newInstance().addCubesLegacy(cube);
        var model = GeoModelOuterClass.GeoModel.newInstance()
                .addBones(GeoModelOuterClass.Bone.newInstance()
                        .setName("root")
                        .addAllPivot(0, 0, 0)
                        .addAllRotate(0, 0, 0)
                        .setCubeCount(1))
                .setCubes(ProtoUtil.serializeToArray(cubes));

        try (var modelData = ArrayBuffer.allocate(model.getSerializedSize());
             var texture = NativeBuffer.allocate(16)) {
            model.writeTo(ProtoUtil.sink(modelData));
            texture.nio().putLong(0, -1L).putLong(8, -1L);
            var baked = NativeBakedModel.bake(modelData, 1, texture.ptr(),
                    2, 2, 29, false);
            try (var bakedData = baked.bakedData()) {
                var read = NativeBakedModel.read(bakedData, 1);
                try (var bakedModel = read.bakedModel()) {
                    var uv = bakedModel.getCubeData(0, 1, 0, 1)[0]
                            .quads()[0].uv();
                    assertArrayEquals(new Vector2f[]{
                            new Vector2f(0, 0), new Vector2f(0.5f, 0),
                            new Vector2f(0.5f, 0.5f), new Vector2f(0, 0.5f)
                    }, uv);
                }
            }
        }
    }

    @Test
    void extractsBorrowedViewsAndCountsWithNoRenderBones() throws Exception {
        var model = GeoModelOuterClass.GeoModel.newInstance();
        model.addBones(GeoModelOuterClass.Bone.newInstance()
                .setName("root")
                .addRotate(0).addRotate(0).addRotate(0)
                .addPivot(0).addPivot(0).addPivot(0));

        try (var modelData = ArrayBuffer.allocate(model.getSerializedSize());
             var texture = NativeBuffer.allocate(4)) {
            model.writeTo(ProtoUtil.sink(modelData));
            var baked = NativeBakedModel.bake(modelData, 1, texture.ptr(),
                    1, 1, 0, false);
            try (var bakedData = baked.bakedData()) {
                var read = NativeBakedModel.read(bakedData, 1);
                try (var bakedModel = read.bakedModel();
                     var state = NativeModelState.create()) {
                    var attributes = new float[AnimatedGeoModel.BONE_ATTRIBUTE_COUNT];
                    attributes[6] = 1;
                    attributes[7] = 1;
                    attributes[8] = 1;
                    attributes[13] = 0xFFFF;

                    assertTrue(state.extract(bakedModel, attributes));
                    assertEquals(1, state.getBonePoses().getBoneCount());
                    assertEquals(0, state.getRenderBoneIndices().capacity());
                    assertTrue(state.getLocatorBoneIndices().isEmpty());
                    assertEquals(0, state.getTotalVertexCount());
                    assertEquals(0, state.getTranslucentVertexCount());

                    state.close();
                    assertFalse(state.isValid());
                    assertNull(state.getBonePoses());
                    assertNull(state.getRenderBoneIndices());
                }
            }
        }
    }
}
