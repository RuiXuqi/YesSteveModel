package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.proto.network.protocol.v0.CommonV0;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelReferenceCodecTest {
    @Test
    void distinguishesIntrinsicDefaultFromRegularHash() {
        var hash = hash(1);
        var regular = CommonV0.ModelReference.newInstance();
        ModelReferenceCodec.write(regular, hash, null);
        assertEquals(hash, ModelReferenceCodec.read(regular));

        var builtin = CommonV0.ModelReference.newInstance();
        ModelReferenceCodec.write(builtin, null, hash);
        assertNull(ModelReferenceCodec.read(builtin));
        assertTrue(ModelReferenceCodec.valid(builtin));
    }

    @Test
    void rejectsAbsentFalseAndTruncatedReferences() {
        assertThrows(IllegalArgumentException.class, () -> ModelReferenceCodec.read(null));
        assertFalse(ModelReferenceCodec.valid(CommonV0.ModelReference.newInstance()));
        assertFalse(ModelReferenceCodec.valid(CommonV0.ModelReference.newInstance()
                .setBuiltinDefault(false)));
        assertFalse(ModelReferenceCodec.valid(CommonV0.ModelReference.newInstance()
                .setModelHash(new byte[ModelHash.SIZE - 1])));
    }

    private static ModelHash hash(int marker) {
        var bytes = new byte[ModelHash.SIZE];
        bytes[bytes.length - 1] = (byte) marker;
        return new ModelHash(bytes);
    }
}
