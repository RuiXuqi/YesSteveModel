package com.elfmcys.ysm.geckolib3.core.molang.storage;

public interface IScopedVariableStorage {
    Object getScoped(int name);
    void setScoped(int name, Object value);
}
