package com.elfmcys.ysm.util;

import com.elfmcys.ysm.model.domain.ModelHash;
import us.hebi.quickbuf.RepeatedByte;

import java.util.Arrays;

public final class ProtoBytes {
    private ProtoBytes() {
    }

    public static byte[] copy(RepeatedByte bytes) {
        return Arrays.copyOf(bytes.array(), bytes.length());
    }

    public static boolean equals(byte[] expected, RepeatedByte actual) {
        return Arrays.equals(expected, 0, expected.length,
                actual.array(), 0, actual.length());
    }

    public static boolean equals(ModelHash expected, RepeatedByte actual) {
        return expected.matches(actual.array(), 0, actual.length());
    }

    /** Gives Quickbuf one owned copy instead of letting a generated setter copy twice. */
    public static void set(RepeatedByte target, ModelHash source) {
        target.setInternalArray(source.bytes());
    }
}
