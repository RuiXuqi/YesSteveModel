package com.elfmcys.ysm.util;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrays;

public final class ExposedShortArrayList extends ShortArrayList {
    public ExposedShortArrayList(int capacity) {
        super(capacity);
    }

    public short[] getUnderlyingArray() {
        return a;
    }

    @Override
    public void size(int size) {
        if (size > this.a.length) {
            this.a = ShortArrays.forceCapacity(this.a, size, this.size);
        }

        this.size = size;
    }
}
