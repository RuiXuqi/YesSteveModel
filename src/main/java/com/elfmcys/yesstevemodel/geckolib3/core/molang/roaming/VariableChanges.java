package com.elfmcys.yesstevemodel.geckolib3.core.molang.roaming;

import java.util.concurrent.ConcurrentHashMap;

public class VariableChanges {
    public final int instanceId;
    public final ConcurrentHashMap<String, Float> variables = new ConcurrentHashMap<>();

    public VariableChanges(int instanceId) {
        this.instanceId = instanceId;
    }
}
