package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.proto.network.protocol.v0.CommonV0;
import org.jetbrains.annotations.Nullable;

public final class ModelReferenceCodec {
    private ModelReferenceCodec() {
    }

    public static void write(CommonV0.ModelReference target,
                             @Nullable ModelHash hash,
                             @Nullable ModelHash builtinDefaultHash) {
        if (hash == null || hash.equals(builtinDefaultHash)) {
            target.setBuiltinDefault(true);
        } else {
            target.setModelHash(hash.bytes());
        }
    }

    /** Returns null for the intrinsic builtin default. */
    public static @Nullable ModelHash read(CommonV0.ModelReference value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing model reference");
        }
        if (value.hasBuiltinDefault() && value.getBuiltinDefault()) {
            return null;
        }
        if (!value.hasModelHash() || value.getModelHash().length() != ModelHash.SIZE) {
            throw new IllegalArgumentException("Invalid model reference");
        }
        return new ModelHash(value.getModelHash().array(), 0,
                value.getModelHash().length());
    }

    public static boolean valid(CommonV0.ModelReference value) {
        return value != null && (value.hasBuiltinDefault() && value.getBuiltinDefault()
                || value.hasModelHash() && value.getModelHash().length() == ModelHash.SIZE);
    }
}
