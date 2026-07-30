package com.elfmcys.ysm.format.schema.baked.model;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.format.schema.file.AssetFileWriter;
import com.elfmcys.ysm.natives.Blake3;
import mixel.asset.model.data.GeoModelOuterClass;

import java.io.IOException;

public class BakedModelWriter extends AssetFileWriter {
    public BakedModelWriter() {
        setSchemaId(BakedModelConstant.SCHEMA_ID);
        setProperty(BakedModelConstant.PROP_VERSION,
                BakedModelConstant.CURRENT_VERSION.toString());
    }

    public void setData(byte[] modelHash,
                        GeoModelOuterClass.GeoModel sourceModel,
                        UniBuffer bakedData) throws IOException {
        if (modelHash.length != Blake3.HASH_SIZE) {
            throw new IllegalArgumentException("Invalid model hash");
        }
        addProtoChunk(BakedModelConstant.MANIFEST_CHUNK_NAME,
                createIndex(sourceModel), 0);
        addRawChunk(BakedModelConstant.MODEL_HASH_CHUNK_NAME,
                ArrayBuffer.borrow(modelHash), 0);
        addRawChunk(BakedModelConstant.MODEL_CHUNK_NAME, bakedData, 9);
    }

    private static GeoModelOuterClass.GeoModelIndex createIndex(
            GeoModelOuterClass.GeoModel source) {
        var index = GeoModelOuterClass.GeoModelIndex.newInstance();
        if (source.hasBones()) {
            index.getMutableBones().addAll(source.getBones());
        }
        if (source.hasProperties()) {
            index.getMutableProperties().copyFrom(source.getProperties());
        }
        if (source.hasLegacyFormat()) {
            index.setLegacyFormat(source.getLegacyFormat());
        }
        return index;
    }
}
