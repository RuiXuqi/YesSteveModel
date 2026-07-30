package com.elfmcys.ysm.client.animation.molang.roaming;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

public class VariableChanges {
    public final int modelHashShort;
    public final Int2FloatOpenHashMap variables;

    public VariableChanges(int modelHashShort, int initSize) {
        this.modelHashShort = modelHashShort;
        this.variables = new Int2FloatOpenHashMap(initSize);
    }
}
