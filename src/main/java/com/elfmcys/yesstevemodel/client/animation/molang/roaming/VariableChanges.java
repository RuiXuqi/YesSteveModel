package com.elfmcys.yesstevemodel.client.animation.molang.roaming;

import java.util.concurrent.ConcurrentHashMap;

public class VariableChanges {
    public final int modelHashShort;
    public final ConcurrentHashMap<Integer, Float> variables = new ConcurrentHashMap<>();

    public VariableChanges(int modelHashShort) {
        this.modelHashShort = modelHashShort;
    }
}
