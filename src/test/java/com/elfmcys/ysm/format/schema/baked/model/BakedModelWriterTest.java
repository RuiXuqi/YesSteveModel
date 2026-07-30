package com.elfmcys.ysm.format.schema.baked.model;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.natives.Blake3;
import mixel.asset.model.data.GeoModelOuterClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BakedModelWriterTest {
    @TempDir
    Path temp;

    @Test
    void writesWithoutAnExplicitSummary() throws Exception {
        var path = temp.resolve("model.geo.ysm-cache");
        var hash = new byte[Blake3.HASH_SIZE];
        var model = GeoModelOuterClass.GeoModel.newInstance();
        model.addBones(GeoModelOuterClass.Bone.newInstance().setName("root"));
        try (var baked = ArrayBuffer.move(new byte[]{1, 2, 3});
             var writer = new BakedModelWriter();
             var output = FileChannel.open(path, StandardOpenOption.CREATE_NEW,
                     StandardOpenOption.WRITE)) {
            writer.setData(hash, model, baked);
            writer.write(output);
        }

        try (var input = FileChannel.open(path, StandardOpenOption.READ)) {
            var view = new BakedModelView(input);
            assertArrayEquals(hash, view.modelHash());
            try (var baked = view.readModelData(input)) {
                assertEquals(3, baked.size());
            }
        }
    }
}
