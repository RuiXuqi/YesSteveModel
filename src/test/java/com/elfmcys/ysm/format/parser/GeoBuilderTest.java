package com.elfmcys.ysm.format.parser;

import com.elfmcys.ysm.format.parser.pojo.model.Bone;
import com.elfmcys.ysm.format.parser.pojo.model.Cube;
import com.elfmcys.ysm.format.parser.pojo.model.CubeUv;
import com.elfmcys.ysm.format.parser.pojo.model.GeoModel;
import com.elfmcys.ysm.format.parser.pojo.model.Geometry;
import mixel.asset.model.data.GeoModelOuterClass;
import org.junit.jupiter.api.Test;
import us.hebi.quickbuf.ProtoSource;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GeoBuilderTest {
    private static final float[] CUBE_NORMALS = new float[]{
            -1, 0, 0,
            1, 0, 0,
            0, 0, -1,
            0, 0, 1,
            0, 1, 0,
            0, -1, 0
    };

    @Test
    void writesNativeCompatibleFullCubeGeometry() throws Exception {
        var cubes = buildCubes(modelWithCubes(cube(null)));
        var encoded = cubes.getCubesLegacy().get(0);

        assertEquals(6, encoded.getFaceCount());
        assertEquals(24, encoded.getPosIndices().length());
        assertEquals(24, encoded.getUvIndices().length());
        assertEquals(18, encoded.getNormal().length());
    }

    @Test
    void cubeRotationDoesNotAffectFollowingCubeNormals() throws Exception {
        var cubes = buildCubes(modelWithCubes(
                cube(new float[]{0, 90, 0}),
                cube(null)));

        assertArrayEquals(CUBE_NORMALS, cubes.getCubesLegacy().get(1).getNormal().toArray());
    }

    @Test
    void repeatedBuildsProduceIdenticalNormals() throws Exception {
        var model = modelWithCubes(cube(new float[]{20, 35, 10}));

        var first = buildCubes(model).getCubesLegacy().get(0).getNormal().toArray();
        var second = buildCubes(model).getCubesLegacy().get(0).getNormal().toArray();

        assertArrayEquals(first, second);
    }

    private static GeoModelOuterClass.Cubes buildCubes(GeoModel model) throws Exception {
        var result = GeoBuilder.build(model);
        return GeoModelOuterClass.Cubes.parseFrom(
                ProtoSource.newInstance(result.model.getCubes()));
    }

    private static GeoModel modelWithCubes(Cube... cubes) {
        var raw = new GeoModel();
        raw.minecraftGeometry = new ArrayList<>();
        var geometry = new Geometry();
        geometry.description.textureWidth = 64;
        geometry.description.textureHeight = 64;
        raw.minecraftGeometry.add(geometry);

        var bone = new Bone();
        bone.name = "root";
        bone.cubes = new ArrayList<>();
        geometry.bones.add(bone);
        bone.cubes.addAll(java.util.List.of(cubes));
        return raw;
    }

    private static Cube cube(float[] rotation) {
        var cube = new Cube();
        cube.origin = new float[]{0, 0, 0};
        cube.size = new float[]{16, 16, 16};
        cube.uv = CubeUv.box(new float[]{0, 0});
        cube.rotation = rotation;
        return cube;
    }
}
