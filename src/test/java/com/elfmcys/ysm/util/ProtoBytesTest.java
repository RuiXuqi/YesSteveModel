package com.elfmcys.ysm.util;

import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.proto.network.model.ModelAssetsProto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtoBytesTest {
    @Test
    void comparesRepeatedBytesWithoutCreatingAnIntermediateArray() {
        var chunk = ModelAssetsProto.ModelChunk.newInstance().setContentHash(new byte[]{1, 2, 3});

        assertTrue(ProtoBytes.equals(new byte[]{1, 2, 3}, chunk.getContentHash()));
        assertFalse(ProtoBytes.equals(new byte[]{1, 2}, chunk.getContentHash()));
        assertFalse(ProtoBytes.equals(new byte[]{1, 2, 4}, chunk.getContentHash()));
    }

    @Test
    void copyReturnsOnlyTheLogicalRepeatedByteRange() {
        var chunk = ModelAssetsProto.ModelChunk.newInstance().setContentHash(new byte[]{4, 5, 6});

        assertArrayEquals(new byte[]{4, 5, 6}, ProtoBytes.copy(chunk.getContentHash()));
    }

    @Test
    void transfersOneOwnedModelHashCopyToQuickbuf() {
        var source = new byte[ModelHash.SIZE];
        source[0] = 42;
        var hash = new ModelHash(source);
        var chunk = ModelAssetsProto.ModelChunk.newInstance();

        ProtoBytes.set(chunk.getMutableContentHash(), hash);
        source[0] = 0;

        assertTrue(ProtoBytes.equals(hash, chunk.getContentHash()));
        assertArrayEquals(hash.bytes(), ProtoBytes.copy(chunk.getContentHash()));
    }
}
